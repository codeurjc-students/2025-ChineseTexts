package com.chinesereads.backend.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

/**
 * Profile update contract. The client sends only the editable fields (name, language,
 * emailConsent), so the endpoint must accept a body where every other UserDTO field is
 * absent — Jackson 3 rejects missing PRIMITIVE fields, which is exactly the regression
 * covered here. Also pins the signup role policy: the server decides the role, never
 * the request body.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ProfileApiTest {

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

    private Map<String, Object> signupPayload(String email, List<String> roles) {
        Map<String, Object> m = new HashMap<>();
        m.put("email", email);
        m.put("name", "N");
        m.put("password", "test1234");
        m.put("language", "en");
        m.put("collections", List.of());
        if (roles != null) m.put("roles", roles);
        m.put("termsAccepted", true);
        return m;
    }

    private Map<String, String> signupAndLogin(String email) {
        given().contentType(ContentType.JSON).body(signupPayload(email, List.of("USER")))
            .when().post("/api/users/signup")
            .then().statusCode(201);

        Response login = given().contentType(ContentType.JSON)
            .body(Map.of("username", email, "password", "test1234"))
            .when().post("/api/auth/login")
            .then().statusCode(200).extract().response();
        return login.getCookies();
    }

    @Test
    @DisplayName("PUT /me accepts the partial body the profile form sends (regression)")
    public void updateProfileWithPartialBodyWorks() {
        Map<String, String> cookies = signupAndLogin("profile@test.com");

        given().contentType(ContentType.JSON).cookies(cookies)
            .body(Map.of("name", "Nuevo Nombre", "language", "es", "emailConsent", true))
            .when().put("/api/users/me")
            .then().statusCode(200)
            .body("name", equalTo("Nuevo Nombre"))
            .body("language", equalTo("es"))
            .body("emailConsent", equalTo(true));
    }

    @Test
    @DisplayName("PUT /me without emailConsent leaves the stored consent unchanged")
    public void updateProfileWithoutConsentKeepsIt() {
        Map<String, String> cookies = signupAndLogin("consent@test.com");

        given().contentType(ContentType.JSON).cookies(cookies)
            .body(Map.of("name", "N", "language", "en", "emailConsent", true))
            .when().put("/api/users/me")
            .then().statusCode(200).body("emailConsent", equalTo(true));

        given().contentType(ContentType.JSON).cookies(cookies)
            .body(Map.of("name", "Renamed", "language", "en"))
            .when().put("/api/users/me")
            .then().statusCode(200)
            .body("name", equalTo("Renamed"))
            .body("emailConsent", equalTo(true));
    }

    @Test
    @DisplayName("Signup ignores client-sent roles: ADMIN in the request still creates a USER")
    public void signupCannotSelfRegisterAsAdmin() {
        given().contentType(ContentType.JSON).body(signupPayload("evil@test.com", List.of("ADMIN")))
            .when().post("/api/users/signup")
            .then().statusCode(201).body("roles", equalTo(List.of("USER")));

        User stored = userRepository.findByEmail("evil@test.com").orElseThrow();
        assertEquals(List.of("USER"), stored.getRoles());
    }

    @Test
    @DisplayName("Signup without any roles field still creates a working USER account")
    public void signupWithoutRolesGetsUserRole() {
        given().contentType(ContentType.JSON).body(signupPayload("norole@test.com", null))
            .when().post("/api/users/signup")
            .then().statusCode(201).body("roles", equalTo(List.of("USER")));
    }
}
