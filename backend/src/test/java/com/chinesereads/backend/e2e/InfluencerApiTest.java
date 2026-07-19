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
import io.restassured.response.Response;

/**
 * Security contract of the influencer endpoints: anonymous callers are rejected, a
 * regular USER is forbidden, and an ADMIN without Stripe configured (the test profile)
 * gets a clean 503 BILLING_UNAVAILABLE — never a stack trace or an accidental success.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class InfluencerApiTest {

    @LocalServerPort
    int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private Map<String, String> adminCookies;
    private Map<String, String> userCookies;

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = this.port;

        userRepository.deleteAll();
        userService.save(new UserDTO(null, "admin@test.com", "Admin", "es",
                List.of(), List.of("ADMIN"), "admin123", null));
        userService.save(new UserDTO(null, "user@test.com", "User", "es",
                List.of(), List.of("USER"), "user123", null));

        adminCookies = login("admin@test.com", "admin123");
        userCookies = login("user@test.com", "user123");
    }

    private Map<String, String> login(String username, String password) {
        Response response = given().contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/api/auth/login")
                .then().statusCode(200).extract().response();
        return response.getCookies();
    }

    @Test
    @DisplayName("Anonymous requests to the influencer endpoints are rejected")
    public void anonymousIsRejected() {
        given().when().get("/api/influencers").then().statusCode(401);
        given().contentType(ContentType.JSON).body(Map.of("code", "X", "percentOff", 10))
                .when().post("/api/influencers").then().statusCode(401);
        given().when().delete("/api/influencers/promo_x").then().statusCode(401);
    }

    @Test
    @DisplayName("A regular USER cannot access the influencer endpoints")
    public void regularUserIsForbidden() {
        given().cookies(userCookies).when().get("/api/influencers")
                .then().statusCode(403);
        given().cookies(userCookies).contentType(ContentType.JSON)
                .body(Map.of("code", "X", "percentOff", 10))
                .when().post("/api/influencers").then().statusCode(403);
    }

    @Test
    @DisplayName("Settlements are ADMIN-only and work even without Stripe configured")
    public void settlementsSecurityAndAvailability() {
        // The settlement is computed from the LOCAL payment ledger, so unlike the other
        // influencer endpoints it must answer even with billing down/unconfigured —
        // past settlements stay reachable no matter what.
        given().when().get("/api/influencers/settlements?from=2026-07-01&to=2026-07-31")
                .then().statusCode(401);
        given().cookies(userCookies)
                .when().get("/api/influencers/settlements?from=2026-07-01&to=2026-07-31")
                .then().statusCode(403);
        given().cookies(adminCookies)
                .when().get("/api/influencers/settlements?from=2026-07-01&to=2026-07-31")
                .then().statusCode(200)
                .body("from", equalTo("2026-07-01"))
                .body("payoutMonthlyCents", equalTo(200))
                .body("rows.size()", equalTo(0));
    }

    @Test
    @DisplayName("A malformed or inverted settlement range is a clean 400 INVALID_RANGE")
    public void settlementsInvalidRange() {
        given().cookies(adminCookies)
                .when().get("/api/influencers/settlements?from=notadate&to=2026-07-31")
                .then().statusCode(400).body("code", equalTo("INVALID_RANGE"));
        given().cookies(adminCookies)
                .when().get("/api/influencers/settlements?from=2026-08-01&to=2026-07-01")
                .then().statusCode(400).body("code", equalTo("INVALID_RANGE"));
    }

    @Test
    @DisplayName("Without Stripe configured an ADMIN gets a clean 503 BILLING_UNAVAILABLE")
    public void adminWithoutStripeGets503() {
        given().cookies(adminCookies).when().get("/api/influencers")
                .then().statusCode(503).body("code", equalTo("BILLING_UNAVAILABLE"));
        given().cookies(adminCookies).contentType(ContentType.JSON)
                .body(Map.of("code", "MARIA", "percentOff", 20, "duration", "once"))
                .when().post("/api/influencers")
                .then().statusCode(503).body("code", equalTo("BILLING_UNAVAILABLE"));
    }
}
