package com.chinesereads.backend.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.PasswordResetService;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * E2E tests for the anonymous forgot/reset password flow. Pins that both endpoints
 * are PUBLIC (the emailed one-shot token is the authentication), the anti-enumeration
 * contract (identical 200 for known and unknown emails), the single-use + expiry
 * semantics of the token, and that a completed reset actually allows logging in with
 * the new password (and blocks the old one).
 *
 * The per-IP rate limit is raised for this class (every request comes from localhost)
 * — its 429 path has its own dedicated test class with a tiny limit.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "password-reset.rate-limit.per-hour=1000")
@ActiveProfiles("test")
public class PasswordResetApiTest {

    @LocalServerPort
    int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = this.port;
        userRepository.deleteAll();

        user = new User("reset@test.com", "Reset User", passwordEncoder.encode("vieja-1234"),
                "es", "USER");
        userRepository.save(user);
    }

    @AfterEach
    public void tearDown() {
        userRepository.deleteAll();
    }

    // Test E2E 1: email existente → 200 y queda un token hasheado con caducidad futura
    @Test
    @DisplayName("POST /forgot-password with a known email returns 200 and stores a hashed token")
    public void testForgotPasswordKnownEmail() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "reset@test.com"))
        .when()
            .post("/api/auth/forgot-password")
        .then()
            .statusCode(200);

        User reloaded = userRepository.findByEmail("reset@test.com").orElseThrow();
        assertNotNull(reloaded.getPasswordResetTokenHash());
        // Solo se persiste el hash SHA-256 (64 hex), jamás el token en claro.
        assertTrue(reloaded.getPasswordResetTokenHash().matches("^[0-9a-f]{64}$"));
        assertTrue(reloaded.getPasswordResetExpiresAt().isAfter(LocalDateTime.now()));
    }

    // Test E2E 2: email desconocido → MISMO 200 y mismo cuerpo (anti-enumeración)
    @Test
    @DisplayName("POST /forgot-password answers identically for unknown emails (no enumeration)")
    public void testForgotPasswordUnknownEmailSameResponse() {
        String knownBody = given().contentType(ContentType.JSON)
                .body(Map.of("email", "reset@test.com"))
                .post("/api/auth/forgot-password").then().statusCode(200).extract().asString();

        String unknownBody = given().contentType(ContentType.JSON)
                .body(Map.of("email", "nadie@test.com"))
                .post("/api/auth/forgot-password").then().statusCode(200).extract().asString();

        assertEquals(knownBody, unknownBody);
    }

    // Test E2E 3: email con formato inválido → 400 con código estable
    @Test
    @DisplayName("POST /forgot-password with a malformed email returns 400 INVALID_EMAIL")
    public void testForgotPasswordInvalidEmail() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "esto-no-es-un-email"))
        .when()
            .post("/api/auth/forgot-password")
        .then()
            .statusCode(400)
            .body("code", equalTo("INVALID_EMAIL"));
    }

    // Test E2E 4: cuenta bloqueada → 200 (sin revelar nada) pero sin token
    @Test
    @DisplayName("POST /forgot-password for a blocked account returns 200 but stores no token")
    public void testForgotPasswordBlockedAccount() {
        user.setBlocked(true);
        userRepository.save(user);

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "reset@test.com"))
        .when()
            .post("/api/auth/forgot-password")
        .then()
            .statusCode(200);

        assertNull(userRepository.findByEmail("reset@test.com").orElseThrow()
                .getPasswordResetTokenHash());
    }

    // Test E2E 5: flujo completo — reset válido cambia la contraseña, permite login
    // con la nueva, bloquea la vieja y consume el token (segundo uso → 400)
    @Test
    @DisplayName("A valid reset changes the password, enables login with it and is single-use")
    public void testFullResetFlow() {
        seedToken("token-e2e-valido", LocalDateTime.now().plusMinutes(30));

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("token", "token-e2e-valido", "newPassword", "nueva-5678"))
        .when()
            .post("/api/auth/reset-password")
        .then()
            .statusCode(200);

        // Login con la contraseña nueva funciona; con la vieja ya no.
        given().contentType(ContentType.JSON)
                .body(Map.of("username", "reset@test.com", "password", "nueva-5678"))
                .post("/api/auth/login").then().statusCode(200);
        given().contentType(ContentType.JSON)
                .body(Map.of("username", "reset@test.com", "password", "vieja-1234"))
                .post("/api/auth/login").then().statusCode(401);

        // Un solo uso: el token quedó consumido.
        assertNull(userRepository.findByEmail("reset@test.com").orElseThrow()
                .getPasswordResetTokenHash());
        given().contentType(ContentType.JSON)
                .body(Map.of("token", "token-e2e-valido", "newPassword", "otra-9999"))
                .post("/api/auth/reset-password").then()
                .statusCode(400).body("code", equalTo("INVALID_OR_EXPIRED_TOKEN"));
    }

    // Test E2E 6: token caducado → 400 y la contraseña vieja sigue valiendo
    @Test
    @DisplayName("An expired token is rejected and the old password keeps working")
    public void testExpiredToken() {
        seedToken("token-e2e-caducado", LocalDateTime.now().minusMinutes(1));

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("token", "token-e2e-caducado", "newPassword", "nueva-5678"))
        .when()
            .post("/api/auth/reset-password")
        .then()
            .statusCode(400)
            .body("code", equalTo("INVALID_OR_EXPIRED_TOKEN"));

        given().contentType(ContentType.JSON)
                .body(Map.of("username", "reset@test.com", "password", "vieja-1234"))
                .post("/api/auth/login").then().statusCode(200);
    }

    // Test E2E 7: token desconocido → 400 con el MISMO código que el caducado (sin pistas)
    @Test
    @DisplayName("An unknown token gets the same error code as an expired one")
    public void testUnknownToken() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("token", "token-inventado", "newPassword", "nueva-5678"))
        .when()
            .post("/api/auth/reset-password")
        .then()
            .statusCode(400)
            .body("code", equalTo("INVALID_OR_EXPIRED_TOKEN"));
    }

    // Test E2E 8: contraseña demasiado corta → 400 y el token NO se consume
    @Test
    @DisplayName("A too-short new password returns 400 and does not consume the token")
    public void testShortPasswordKeepsToken() {
        seedToken("token-e2e-corto", LocalDateTime.now().plusMinutes(30));

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("token", "token-e2e-corto", "newPassword", "abc"))
        .when()
            .post("/api/auth/reset-password")
        .then()
            .statusCode(400)
            .body("code", equalTo("PASSWORD_TOO_SHORT"));

        assertNotNull(userRepository.findByEmail("reset@test.com").orElseThrow()
                .getPasswordResetTokenHash()); // sigue vivo para reintentar
    }

    /** Siembra en BD el hash de un token conocido, como haría requestReset. */
    private void seedToken(String rawToken, LocalDateTime expiresAt) {
        User u = userRepository.findByEmail("reset@test.com").orElseThrow();
        u.setPasswordResetTokenHash(PasswordResetService.sha256(rawToken));
        u.setPasswordResetExpiresAt(expiresAt);
        userRepository.save(u);
    }
}
