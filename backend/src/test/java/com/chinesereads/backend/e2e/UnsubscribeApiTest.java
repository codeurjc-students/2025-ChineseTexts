package com.chinesereads.backend.e2e;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;

import io.restassured.RestAssured;

/**
 * E2E tests for the one-click email unsubscribe. Pins that the endpoint is PUBLIC
 * (the visitor comes from a mail client with no session — the token is the auth),
 * that it actually withdraws consent, and that unknown tokens leak nothing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UnsubscribeApiTest {

    @LocalServerPort
    int port;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = this.port;
        userRepository.deleteAll();

        user = new User();
        user.setEmail("consent@test.com");
        user.setName("Consent User");
        user.setLanguage("es");
        user.setEmailConsent(true);
        user.setEmailConsentAt(LocalDateTime.now());
        user.setUnsubscribeToken("e2e-token-123");
        userRepository.save(user);
    }

    @AfterEach
    public void tearDown() {
        userRepository.deleteAll();
    }

    // Test E2E 1: la baja funciona sin autenticación y retira el consentimiento
    @Test
    @DisplayName("GET /api/users/unsubscribe with a valid token withdraws consent without login")
    public void testUnsubscribeWithValidToken() {
        given()
        .when()
            .get("/api/users/unsubscribe?token=e2e-token-123")
        .then()
            .statusCode(200)
            .contentType(containsString("text/html"))
            .body(containsString("Te has dado de baja")); // página en el idioma del usuario

        User reloaded = userRepository.findByEmail("consent@test.com").orElseThrow();
        assertFalse(reloaded.isEmailConsent());
        assertNull(reloaded.getEmailConsentAt()); // misma semántica que revocar desde el perfil
    }

    // Test E2E 2: un token desconocido devuelve 404 y no toca a nadie
    @Test
    @DisplayName("GET /api/users/unsubscribe with an unknown token returns 404 and changes nothing")
    public void testUnsubscribeWithUnknownToken() {
        given()
        .when()
            .get("/api/users/unsubscribe?token=no-existe")
        .then()
            .statusCode(404);

        assertTrue(userRepository.findByEmail("consent@test.com").orElseThrow().isEmailConsent());
    }
}
