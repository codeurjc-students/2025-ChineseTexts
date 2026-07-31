package com.chinesereads.backend.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Dedicated class for the 429 path of POST /api/auth/forgot-password: with the limit
 * shrunk to 2/hour, the third request from the same IP must be rejected. Lives apart
 * from PasswordResetApiTest because the limiter counts per IP and every test request
 * comes from localhost — a shared context would trip the other tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "password-reset.rate-limit.per-hour=2")
@ActiveProfiles("test")
public class PasswordResetRateLimitApiTest {

    @LocalServerPort
    int port;

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = this.port;
    }

    // Test E2E 1: a la tercera petición de la misma IP responde 429 con código estable
    @Test
    @DisplayName("The third forgot-password request from the same IP gets 429")
    public void testThirdRequestIsRateLimited() {
        for (int i = 0; i < 2; i++) {
            given().contentType(ContentType.JSON)
                    .body(Map.of("email", "cualquiera@test.com"))
                    .post("/api/auth/forgot-password").then().statusCode(200);
        }
        given().contentType(ContentType.JSON)
                .body(Map.of("email", "cualquiera@test.com"))
        .when()
                .post("/api/auth/forgot-password")
        .then()
                .statusCode(429)
                .body("code", equalTo("TOO_MANY_REQUESTS"));
    }
}
