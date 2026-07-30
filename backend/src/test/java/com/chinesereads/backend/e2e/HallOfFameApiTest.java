package com.chinesereads.backend.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.chinesereads.backend.Repository.HallOfFameEntryRepository;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.UserService;
import com.chinesereads.backend.dto.UserDTO;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Hall of Fame API contract: public GETs, ADMIN-only writes (the security
 * matchers are the safety net — the chain ends in permitAll), badge whitelist
 * validation, slug rules, and the Jackson 3 partial-body regression (wrapper
 * fields omitted = unchanged), mirroring {@link FounderApiTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class HallOfFameApiTest {

    @LocalServerPort
    int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HallOfFameEntryRepository entryRepository;

    @Autowired
    private UserService userService;

    private Map<String, String> adminCookies;

    private Map<String, String> userCookies;

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = this.port;
        entryRepository.deleteAll();
        userRepository.deleteAll();

        userService.save(new UserDTO(null, "admin@test.com", "Admin", "es",
                List.of(), List.of("ADMIN"), "admin123", null));
        userService.save(new UserDTO(null, "user@test.com", "User", "es",
                List.of(), List.of("USER"), "user123", null));

        adminCookies = login("admin@test.com", "admin123");
        userCookies = login("user@test.com", "user123");
    }

    private Map<String, String> login(String username, String password) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/api/auth/login")
                .then().statusCode(200).extract().response().getCookies();
    }

    private int createEntry(String name) {
        return given().contentType(ContentType.JSON).cookies(adminCookies)
                .body(Map.of("name", name))
                .when().post("/api/hall-of-fame")
                .then().statusCode(201).extract().path("id");
    }

    @Test
    @DisplayName("GET /api/hall-of-fame is public and returns an empty list")
    public void listIsPublic() {
        given().when().get("/api/hall-of-fame")
                .then().statusCode(200).body("", hasSize(0));
    }

    @Test
    @DisplayName("Writes are rejected for anonymous (401) and USER (403)")
    public void writesRequireAdmin() {
        given().contentType(ContentType.JSON)
                .body(Map.of("name", "Maria"))
                .when().post("/api/hall-of-fame")
                .then().statusCode(401);

        given().contentType(ContentType.JSON).cookies(userCookies)
                .body(Map.of("name", "Maria"))
                .when().post("/api/hall-of-fame")
                .then().statusCode(403);
    }

    @Test
    @DisplayName("Admin creates an entry: slug derived from the name, badges normalized")
    public void adminCreatesEntry() {
        given().contentType(ContentType.JSON).cookies(adminCookies)
                .body(Map.of("name", "María López",
                        "taglineEn", "Chinese teacher",
                        "taglineEs", "Profesora de chino",
                        "discountCode", "MARIA10",
                        // Sent out of canonical order and duplicated on purpose.
                        "badges", List.of("star", "pioneer", "star")))
                .when().post("/api/hall-of-fame")
                .then().statusCode(201)
                .body("slug", equalTo("maria-lopez"))
                .body("displayOrder", equalTo(0))
                .body("badges", contains("pioneer", "star"));
    }

    @Test
    @DisplayName("Unknown badge key is rejected with 400")
    public void unknownBadgeRejected() {
        given().contentType(ContentType.JSON).cookies(adminCookies)
                .body(Map.of("name", "Maria", "badges", List.of("legend")))
                .when().post("/api/hall-of-fame")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("PUT with a partial body only touches the provided fields (regression)")
    public void partialUpdateKeepsOtherFields() {
        int id = given().contentType(ContentType.JSON).cookies(adminCookies)
                .body(Map.of("name", "Maria", "bioEn", "Hello", "badges", List.of("star")))
                .when().post("/api/hall-of-fame")
                .then().statusCode(201).extract().path("id");

        given().contentType(ContentType.JSON).cookies(adminCookies)
                .body(Map.of("taglineEn", "New tagline"))
                .when().put("/api/hall-of-fame/" + id)
                .then().statusCode(200)
                .body("taglineEn", equalTo("New tagline"))
                .body("name", equalTo("Maria"))
                .body("bioEn", equalTo("Hello"))
                .body("displayOrder", equalTo(0))
                .body("badges", contains("star"));
    }

    @Test
    @DisplayName("An explicit duplicate slug is rejected with 400")
    public void duplicateSlugRejected() {
        createEntry("Maria");
        int otherId = createEntry("Laura");

        given().contentType(ContentType.JSON).cookies(adminCookies)
                .body(Map.of("slug", "maria"))
                .when().put("/api/hall-of-fame/" + otherId)
                .then().statusCode(400);
    }

    @Test
    @DisplayName("Photo: admin uploads it, anyone reads it, delete leaves a 404")
    public void photoLifecycle() {
        int id = createEntry("Maria");

        given().when().get("/api/hall-of-fame/" + id + "/photo")
                .then().statusCode(404);

        given().cookies(adminCookies)
                .multiPart("image", "photo.jpg", new byte[]{1, 2, 3}, "image/jpeg")
                .when().post("/api/hall-of-fame/" + id + "/photo")
                .then().statusCode(204);

        given().when().get("/api/hall-of-fame/" + id + "/photo")
                .then().statusCode(200).contentType("image/jpeg");

        given().cookies(adminCookies)
                .when().delete("/api/hall-of-fame/" + id + "/photo")
                .then().statusCode(204);

        given().when().get("/api/hall-of-fame/" + id + "/photo")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("Socials: create with auto displayOrder, partial update keeps it, delete")
    public void socialsLifecycle() {
        int entryId = createEntry("Maria");

        int socialId = given().contentType(ContentType.JSON).cookies(adminCookies)
                .body(Map.of("label", "Instagram", "icon", "bi-instagram", "url", "https://i.g"))
                .when().post("/api/hall-of-fame/" + entryId + "/socials")
                .then().statusCode(201)
                .body("displayOrder", equalTo(0)).extract().path("id");

        given().contentType(ContentType.JSON).cookies(adminCookies)
                .body(Map.of("label", "IG"))
                .when().put("/api/hall-of-fame/socials/" + socialId)
                .then().statusCode(200)
                .body("label", equalTo("IG"))
                .body("url", equalTo("https://i.g"))
                .body("displayOrder", equalTo(0));

        given().cookies(adminCookies)
                .when().delete("/api/hall-of-fame/socials/" + socialId)
                .then().statusCode(204);
    }

    @Test
    @DisplayName("Deleting an entry removes it (and its socials/badges in cascade)")
    public void deleteEntryCascades() {
        int id = createEntry("Maria");
        given().contentType(ContentType.JSON).cookies(adminCookies)
                .body(Map.of("label", "Instagram", "icon", "bi-instagram", "url", "https://i.g"))
                .when().post("/api/hall-of-fame/" + id + "/socials")
                .then().statusCode(201);

        given().cookies(adminCookies)
                .when().delete("/api/hall-of-fame/" + id)
                .then().statusCode(204);

        given().when().get("/api/hall-of-fame")
                .then().statusCode(200).body("", hasSize(0));
    }
}
