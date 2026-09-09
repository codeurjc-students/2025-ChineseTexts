package com.chinesereads.backend.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import io.restassured.response.Response;

/**
 * Blocking a user from the admin panel must end their CURRENT session, not only
 * prevent future logins: the cookies issued before the block stop working on the very
 * next request, the browser is told to delete them, login and refresh keep rejecting
 * the account, and unblocking restores access. Also pins that blocking one user does
 * not disturb anybody else's live session.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AccountBlockApiTest {

    @LocalServerPort
    int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private Map<String, String> adminCookies;
    private Map<String, String> userCookies;
    private long userId;

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = this.port;
        userRepository.deleteAll();

        userService.save(new UserDTO(null, "admin@test.com", "Admin", "es", List.of(), List.of("ADMIN"), "admin123", null));
        userService.save(new UserDTO(null, "user@test.com", "User", "es", List.of(), List.of("USER"), "user123", null));
        userId = userRepository.findByEmail("user@test.com").orElseThrow().getId();

        adminCookies = login("admin@test.com", "admin123");
        userCookies = login("user@test.com", "user123");
    }

    private Map<String, String> login(String email, String password) {
        return given().contentType(ContentType.JSON)
            .body(Map.of("username", email, "password", password))
            .when().post("/api/auth/login")
            .then().statusCode(200).body("status", equalTo("SUCCESS"))
            .extract().response().getCookies();
    }

    private void setBlocked(boolean blocked) {
        given().contentType(ContentType.JSON).cookies(adminCookies)
            .body(Map.of("blocked", blocked))
            .when().patch("/api/users/" + userId + "/blocked")
            .then().statusCode(200).body("blocked", equalTo(blocked));
    }

    @Test
    @DisplayName("Blocking a user kills the session they already had: next request is 401 and cookies are expired")
    public void blockEndsTheActiveSession() {
        // Sanity: the session works before the block.
        given().cookies(userCookies).when().get("/api/users/me")
            .then().statusCode(200).body("email", equalTo("user@test.com"));

        setBlocked(true);

        Response after = given().cookies(userCookies).when().get("/api/users/me")
            .then().statusCode(401).extract().response();
        // Raw Set-Cookie headers: Tomcat renders a max-age-0 cookie as an empty value
        // that expired in 1970, which is what makes the browser delete it.
        List<String> setCookies = after.headers().getValues("Set-Cookie");
        assertTrue(setCookies.stream().anyMatch(h -> h.matches("AuthToken=; Expires=.*1970.*")),
                "access cookie must be deleted, got " + setCookies);
        assertTrue(setCookies.stream().anyMatch(h -> h.matches("RefreshToken=; Expires=.*1970.*")),
                "refresh cookie must be deleted, got " + setCookies);
    }

    @Test
    @DisplayName("A blocked user cannot get back in: login is 403 and the refresh token is rejected")
    public void blockedUserCannotReenter() {
        setBlocked(true);

        given().contentType(ContentType.JSON)
            .body(Map.of("username", "user@test.com", "password", "user123"))
            .when().post("/api/auth/login")
            .then().statusCode(403).body("status", equalTo("FAILURE"));

        given().cookies(userCookies).when().post("/api/auth/refresh")
            .then().statusCode(403).body("status", equalTo("FAILURE"));
    }

    @Test
    @DisplayName("Blocking one user leaves other live sessions untouched, and unblocking restores login")
    public void otherSessionsUnaffectedAndUnblockRestoresAccess() {
        setBlocked(true);

        // The admin's own session (another user) keeps working normally.
        given().cookies(adminCookies).when().get("/api/users/me")
            .then().statusCode(200).body("email", equalTo("admin@test.com"));

        setBlocked(false);

        // Old cookies were deleted client-side, so the user logs in again — and can.
        Map<String, String> fresh = login("user@test.com", "user123");
        given().cookies(fresh).when().get("/api/users/me")
            .then().statusCode(200).body("email", equalTo("user@test.com"));
    }
}
