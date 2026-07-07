package com.chinesereads.backend.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.HashMap;
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

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Signup endpoint contract: GDPR consent is enforced (400 TERMS_NOT_ACCEPTED without it),
 * a duplicate email is a 409 Conflict (a distinct status so the client can show the right
 * message even where the JSON error body is replaced), and a valid, consented signup 201s.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SignupApiTest {

    @LocalServerPort
    int port;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = this.port;
        userRepository.deleteAll();
    }

    private Map<String, Object> payload(String email, String password, Boolean termsAccepted) {
        Map<String, Object> m = new HashMap<>();
        m.put("email", email);
        m.put("name", "N");
        m.put("password", password);
        m.put("language", "en");
        m.put("collections", List.of());
        m.put("roles", List.of("USER"));
        if (termsAccepted != null) m.put("termsAccepted", termsAccepted);
        return m;
    }

    @Test
    @DisplayName("Invalid email is rejected with a stable code")
    public void invalidEmailReturnsCode() {
        given().contentType(ContentType.JSON).body(payload("notanemail", "test1234", true))
            .when().post("/api/users/signup")
            .then().statusCode(400).body("code", equalTo("INVALID_EMAIL"));
    }

    @Test
    @DisplayName("Signup without accepting the terms is rejected (400 TERMS_NOT_ACCEPTED)")
    public void noConsentReturnsTermsCode() {
        given().contentType(ContentType.JSON).body(payload("a@b.com", "test1234", false))
            .when().post("/api/users/signup")
            .then().statusCode(400).body("code", equalTo("TERMS_NOT_ACCEPTED"));
    }

    @Test
    @DisplayName("A valid, consented signup creates the account")
    public void withConsentCreatesAccount() {
        given().contentType(ContentType.JSON).body(payload("newuser@b.com", "test1234", true))
            .when().post("/api/users/signup")
            .then().statusCode(201);
    }

    @Test
    @DisplayName("A duplicate email is a 409 Conflict")
    public void duplicateEmailReturnsConflict() {
        given().contentType(ContentType.JSON).body(payload("dup@b.com", "test1234", true))
            .when().post("/api/users/signup")
            .then().statusCode(201);

        given().contentType(ContentType.JSON).body(payload("dup@b.com", "test1234", true))
            .when().post("/api/users/signup")
            .then().statusCode(409).body("code", equalTo("EMAIL_IN_USE"));
    }
}
