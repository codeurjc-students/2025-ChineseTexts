package com.chinesereads.backend.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.chinesereads.backend.Repository.BlogImageRepository;
import com.chinesereads.backend.Repository.BlogPostRepository;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.UserService;
import com.chinesereads.backend.dto.UserDTO;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Blog API contract: public reads limited to PUBLISHED posts, ADMIN-only
 * writes and admin reads (the catch-all /api/blog/** matcher is the safety
 * net — the chain ends in permitAll), slug rules, publish semantics, the
 * server-side HTML sanitization and the cover/inline-image lifecycles,
 * mirroring {@link HallOfFameApiTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class BlogApiTest {

    @LocalServerPort
    int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlogPostRepository postRepository;

    @Autowired
    private BlogImageRepository imageRepository;

    @Autowired
    private UserService userService;

    private Map<String, String> adminCookies;

    private Map<String, String> userCookies;

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = this.port;
        imageRepository.deleteAll();
        postRepository.deleteAll();
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

    private int createPost(Map<String, Object> body) {
        return given().contentType(ContentType.JSON).cookies(adminCookies)
                .body(body)
                .when().post("/api/blog")
                .then().statusCode(201).extract().path("id");
    }

    @Test
    @DisplayName("GET /api/blog is public and only lists PUBLISHED posts")
    public void publicListOnlyShowsPublished() {
        createPost(Map.of("titleEn", "Draft post"));
        createPost(Map.of("titleEn", "Live post", "published", true));

        given().when().get("/api/blog")
                .then().statusCode(200)
                .body("", hasSize(1))
                .body("[0].titleEn", equalTo("Live post"))
                .body("[0].publishedOn", notNullValue());
    }

    @Test
    @DisplayName("GET /api/blog/slug/{slug} serves a published post and 404s a draft")
    public void slugDetailIsPublishedOnly() {
        createPost(Map.of("titleEn", "My draft", "slug", "my-draft"));
        createPost(Map.of("titleEn", "My article", "slug", "my-article", "published", true));

        given().when().get("/api/blog/slug/my-article")
                .then().statusCode(200)
                .body("titleEn", equalTo("My article"))
                .body("published", equalTo(true));

        given().when().get("/api/blog/slug/my-draft")
                .then().statusCode(404);

        given().when().get("/api/blog/slug/does-not-exist")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("Writes are rejected for anonymous (401) and USER (403)")
    public void writesRequireAdmin() {
        given().contentType(ContentType.JSON)
                .body(Map.of("titleEn", "Nope"))
                .when().post("/api/blog")
                .then().statusCode(401);

        given().contentType(ContentType.JSON).cookies(userCookies)
                .body(Map.of("titleEn", "Nope"))
                .when().post("/api/blog")
                .then().statusCode(403);
    }

    @Test
    @DisplayName("Admin-only reads (/all and /{id}) are rejected for anonymous and USER")
    public void adminReadsRequireAdmin() {
        int id = createPost(Map.of("titleEn", "Secret draft"));

        given().when().get("/api/blog/all").then().statusCode(401);
        given().cookies(userCookies).when().get("/api/blog/all").then().statusCode(403);

        given().when().get("/api/blog/" + id).then().statusCode(401);
        given().cookies(userCookies).when().get("/api/blog/" + id).then().statusCode(403);

        given().cookies(adminCookies).when().get("/api/blog/all")
                .then().statusCode(200).body("", hasSize(1));
        given().cookies(adminCookies).when().get("/api/blog/" + id)
                .then().statusCode(200).body("titleEn", equalTo("Secret draft"));
    }

    @Test
    @DisplayName("Slug is derived from the title (diacritics stripped) and auto-suffixed on collision")
    public void slugDerivationAndAutoSuffix() {
        given().contentType(ContentType.JSON).cookies(adminCookies)
                .body(Map.of("titleEn", "Cómo aprender chino"))
                .when().post("/api/blog")
                .then().statusCode(201).body("slug", equalTo("como-aprender-chino"));

        given().contentType(ContentType.JSON).cookies(adminCookies)
                .body(Map.of("titleEn", "Cómo aprender chino"))
                .when().post("/api/blog")
                .then().statusCode(201).body("slug", equalTo("como-aprender-chino-2"));
    }

    @Test
    @DisplayName("An explicit duplicate slug is a 400 with an error body")
    public void explicitDuplicateSlugIs400() {
        createPost(Map.of("titleEn", "First", "slug", "the-slug"));

        given().contentType(ContentType.JSON).cookies(adminCookies)
                .body(Map.of("titleEn", "Second", "slug", "the-slug"))
                .when().post("/api/blog")
                .then().statusCode(400).body("error", containsString("the-slug"));
    }

    @Test
    @DisplayName("A post without any title is a 400")
    public void titleIsRequired() {
        given().contentType(ContentType.JSON).cookies(adminCookies)
                .body(Map.of("excerptEn", "No title here"))
                .when().post("/api/blog")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("Partial PUT: omitted fields stay unchanged (Jackson 3 wrappers regression)")
    public void partialUpdateKeepsOtherFields() {
        int id = createPost(Map.of("titleEn", "Original title",
                "excerptEn", "Original excerpt", "contentEn", "<p>Body</p>"));

        given().contentType(ContentType.JSON).cookies(adminCookies)
                .body(Map.of("excerptEn", "New excerpt"))
                .when().put("/api/blog/" + id)
                .then().statusCode(200)
                .body("titleEn", equalTo("Original title"))
                .body("excerptEn", equalTo("New excerpt"))
                .body("contentEn", equalTo("<p>Body</p>"));
    }

    @Test
    @DisplayName("Publishing sets publishedOn once; unpublish + republish keeps the original date")
    public void publishSemantics() {
        int id = createPost(Map.of("titleEn", "To publish"));

        String firstDate = given().contentType(ContentType.JSON).cookies(adminCookies)
                .body(Map.of("published", true))
                .when().put("/api/blog/" + id)
                .then().statusCode(200)
                .body("published", equalTo(true))
                .body("publishedOn", notNullValue())
                .extract().path("publishedOn");

        given().contentType(ContentType.JSON).cookies(adminCookies)
                .body(Map.of("published", false))
                .when().put("/api/blog/" + id)
                .then().statusCode(200).body("published", equalTo(false));

        given().contentType(ContentType.JSON).cookies(adminCookies)
                .body(Map.of("published", true))
                .when().put("/api/blog/" + id)
                .then().statusCode(200).body("publishedOn", equalTo(firstDate));
    }

    @Test
    @DisplayName("Content HTML is sanitized on save: scripts, handlers and external images removed")
    public void contentIsSanitized() {
        int id = createPost(Map.of("titleEn", "Sanitized",
                "contentEn", "<h2>Hello</h2><script>alert(1)</script>"
                        + "<p onclick=\"steal()\">Text</p>"
                        + "<img src=\"https://evil.example.com/x.png\">"
                        + "<img src=\"/api/blog/images/7\" alt=\"ok\">"));

        given().cookies(adminCookies).when().get("/api/blog/" + id)
                .then().statusCode(200)
                .body("contentEn", containsString("<h2>Hello</h2>"))
                .body("contentEn", not(containsString("script")))
                .body("contentEn", not(containsString("onclick")))
                .body("contentEn", not(containsString("evil.example.com")))
                .body("contentEn", containsString("/api/blog/images/7"));
    }

    @Test
    @DisplayName("Cover lifecycle: upload (admin), public GET, delete → 404")
    public void coverLifecycle() {
        int id = createPost(Map.of("titleEn", "With cover", "published", true));

        given().when().get("/api/blog/" + id + "/cover").then().statusCode(404);

        given().cookies(adminCookies)
                .multiPart("image", "cover.jpg", new byte[] {1, 2, 3}, "image/jpeg")
                .when().put("/api/blog/" + id + "/cover")
                .then().statusCode(204);

        given().when().get("/api/blog/" + id + "/cover")
                .then().statusCode(200).header("Content-Type", containsString("image/jpeg"));

        given().when().get("/api/blog")
                .then().statusCode(200).body("[0].hasCover", equalTo(true));

        given().cookies(adminCookies)
                .when().delete("/api/blog/" + id + "/cover")
                .then().statusCode(204);

        given().when().get("/api/blog/" + id + "/cover").then().statusCode(404);
    }

    @Test
    @DisplayName("Inline images: admin upload returns its URL, public GET works, deleting the post deletes them")
    public void inlineImageLifecycle() {
        int postId = createPost(Map.of("titleEn", "With images"));

        Integer imageId = given().cookies(adminCookies)
                .multiPart("image", "pic.png", new byte[] {9, 9, 9}, "image/png")
                .formParam("postId", postId)
                .when().post("/api/blog/images")
                .then().statusCode(201)
                .body("url", containsString("/api/blog/images/"))
                .extract().path("id");

        given().when().get("/api/blog/images/" + imageId).then().statusCode(200);

        // Anonymous upload is rejected by the catch-all matcher.
        given().multiPart("image", "pic.png", new byte[] {1}, "image/png")
                .when().post("/api/blog/images")
                .then().statusCode(401);

        given().cookies(adminCookies)
                .when().delete("/api/blog/" + postId)
                .then().statusCode(204);

        given().when().get("/api/blog/images/" + imageId).then().statusCode(404);
        given().cookies(adminCookies).when().get("/api/blog/" + postId).then().statusCode(404);
    }
}
