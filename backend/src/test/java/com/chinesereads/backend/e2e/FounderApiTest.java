package com.chinesereads.backend.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.UserService;
import com.chinesereads.backend.dto.UserDTO;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Founder editor contract under Jackson 3: these DTOs may arrive as PARTIAL JSON
 * bodies, and Jackson 3 rejects any body that omits a primitive field — so
 * displayOrder / hasPhoto / hasLogo must stay wrapper types (omitted = unchanged).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class FounderApiTest {

    @LocalServerPort
    int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private Map<String, String> adminCookies;

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = this.port;
        userRepository.deleteAll();

        userService.save(new UserDTO(null, "admin@test.com", "Admin", "es",
                List.of(), List.of("ADMIN"), "admin123", null));

        adminCookies = given().contentType(ContentType.JSON)
                .body(Map.of("username", "admin@test.com", "password", "admin123"))
                .when().post("/api/auth/login")
                .then().statusCode(200).extract().response().getCookies();
    }

    @Test
    @DisplayName("PUT /api/founder accepts a body without hasPhoto (regression)")
    public void updateProfileWithoutHasPhotoWorks() {
        given().contentType(ContentType.JSON).cookies(adminCookies)
            .body(Map.of("name", "Founder Name", "role", "Creator"))
            .when().put("/api/founder")
            .then().statusCode(200).body("name", equalTo("Founder Name"));
    }

    @Test
    @DisplayName("Updating a social link without displayOrder keeps its position")
    public void updateSocialWithoutDisplayOrderKeepsIt() {
        int id = given().contentType(ContentType.JSON).cookies(adminCookies)
            .body(Map.of("label", "GitHub", "icon", "bi-github", "url", "https://g.h"))
            .when().post("/api/founder/socials")
            .then().statusCode(201).extract().path("id");

        given().contentType(ContentType.JSON).cookies(adminCookies)
            .body(Map.of("label", "GitHub 2", "icon", "bi-github", "url", "https://g.h"))
            .when().put("/api/founder/socials/" + id)
            .then().statusCode(200)
            .body("label", equalTo("GitHub 2"))
            .body("displayOrder", equalTo(0));
    }
}
