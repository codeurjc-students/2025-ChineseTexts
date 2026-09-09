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
 * The 429 path of POST /api/auth/login. With the per-email budget shrunk to 2 failed
 * attempts, the third try is refused even with the right password, other accounts
 * keep working, and a correct login wipes the account's slate. The per-IP cap is
 * raised far above what these tests generate: every request here comes from
 * localhost and the class shares one limiter, so a low IP cap would make the tests
 * order-dependent (the per-IP rule itself is pinned by LoginRateLimiterServiceTest).
 * Lives in its own class so the shrunk limits never leak into other E2E contexts.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = { "login.rate-limit.per-email=2", "login.rate-limit.per-ip=1000" })
@ActiveProfiles("test")
public class LoginRateLimitApiTest {

    @LocalServerPort
    int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = this.port;
        userRepository.deleteAll();
        // One account per test: the class shares a single limiter and JUnit does not
        // promise method order, so no two tests may touch the same email.
        for (String who : List.of("ana", "bea", "carla", "dani")) {
            userService.save(new UserDTO(null, who + "@test.com", who, "es", List.of(), List.of("USER"), "correct-1234", null));
        }
    }

    private io.restassured.response.ValidatableResponse login(String email, String password) {
        return given().contentType(ContentType.JSON)
            .body(Map.of("username", email, "password", password))
            .when().post("/api/auth/login").then();
    }

    @Test
    @DisplayName("A wrong password is a plain 401 FAILURE, identical for an unknown email (no enumeration)")
    public void wrongPasswordIs401() {
        login("ana@test.com", "nope").statusCode(401).body("status", equalTo("FAILURE"));
        login("nobody@test.com", "nope").statusCode(401).body("status", equalTo("FAILURE"));
    }

    @Test
    @DisplayName("After the failed-attempt budget the login is 429 even with the right password; other accounts are unaffected")
    public void thirdAttemptIsRateLimited() {
        login("bea@test.com", "wrong-1").statusCode(401);
        login("bea@test.com", "wrong-2").statusCode(401);

        login("bea@test.com", "correct-1234").statusCode(429).body("status", equalTo("FAILURE"));

        // Another account from the same client still logs in: the cap hit was per email.
        login("dani@test.com", "correct-1234").statusCode(200).body("status", equalTo("SUCCESS"));
    }

    @Test
    @DisplayName("A correct login clears the account's failed attempts")
    public void successResetsTheAccountBudget() {
        login("carla@test.com", "typo").statusCode(401);
        login("carla@test.com", "correct-1234").statusCode(200);

        // Budget is full again: two more typos are tolerated before the 429.
        login("carla@test.com", "typo").statusCode(401);
        login("carla@test.com", "typo").statusCode(401);
        login("carla@test.com", "correct-1234").statusCode(429);
    }
}
