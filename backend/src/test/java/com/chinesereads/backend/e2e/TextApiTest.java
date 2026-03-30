package com.chinesereads.backend.e2e;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.chinesereads.backend.Repository.TextRepository;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.UserService;
import com.chinesereads.backend.dto.UserDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import net.minidev.json.JSONObject;

import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class TextApiTest {

    @LocalServerPort
    int port;

    @Autowired
    private TextRepository textRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private Map<String, String> adminCookies;
    private Map<String, String> userCookies;

    private JSONObject body;

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = this.port;

        userRepository.deleteAll();
        textRepository.deleteAll();

        UserDTO adminDTO = mockAdminUser();
        userService.save(adminDTO);
        userService.save(mockRegularUser());
        body = setBody();

        adminCookies = loginAndGetAllCookies("admin@test.com", "admin123");
        userCookies = loginAndGetAllCookies("user@test.com", "user123");
    }

    private UserDTO mockAdminUser() {
        return new UserDTO(null, "admin@test.com", "Admin","es", List.of(), List.of("ADMIN"), "admin123", null);
    }

    private UserDTO mockRegularUser() {
        return new UserDTO(null, "user@test.com", "User","es", List.of(), List.of("USER"), "user123", null);
    }

    private Map<String, String> loginAndGetAllCookies(String username, String password) {
        Map<String, String> loginRequest = Map.of(
            "username", username,
            "password", password
        );

        Response loginResponse = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .body("status", equalTo("SUCCESS"))
            .extract()
            .response();

        
        Map<String, String> cookies = loginResponse.getCookies();

        //System.out.println("Received cookies: " + cookies);
        
        if (cookies.isEmpty()) {
            throw new RuntimeException("No cookies were received from the login");
        }
        
        return cookies;
    }

    private JSONObject setBody() {
        JSONObject body = new JSONObject();
        body.put("titleEnglish", "Test Title.");
        body.put("titleSpanish", "Titulo de Prueba.");
        body.put("text", "你好。");
        body.put("spanishTranslation", "Hola.");
        body.put("englishTranslation", "Hello.");
        body.put("level", "HSK1");
        body.put("spanishDescription", "Descripción de prueba.");
        body.put("englishDescription", "Descripción de prueba.");
        body.put("creationDate", "2026-01-01");
        body.put("image", null);
        return body;
    }

    @AfterEach
    public void tearDown() {
        userRepository.deleteAll();
        textRepository.deleteAll();
    }

    // Test E2E 1: GET /api/texts devuelve 200 y una lista paginada de tamaño menor o igual a 10 para cualquier usuario
    @Test
    @DisplayName("GET /api/texts should return 200 and a paginated list for any user")
    public void testGetTextsReturns200() {
        Response response = given()
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/texts")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .extract().response();

        // Extrae la lista de TextDTOs
        List<?> texts = response.jsonPath().getList("");

        // Assertions
        assertThat(texts, notNullValue());
        assertThat(texts.size(), lessThanOrEqualTo(10)); // máximo 10 elementos
    }

    // Test E2E 2: POST /api/texts devuelve 201 creado cuando un admin sube un texto con imagen opcional
    @Test
    @DisplayName("POST /api/texts should return 201 when an admin uploads a text with optional image")
    public void testUploadTextWithAdmin() throws Exception {
        // Serializamos el TextDTO a JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String textJson = objectMapper.writeValueAsString(body); // body es tu JSONObject con los campos de TextDTO

        given()
            .cookies(adminCookies) // cookies de admin
            .contentType("multipart/form-data")
            // parte "data" con JSON
            .multiPart("data", "data.json", textJson.getBytes(), "application/json")
            // parte opcional "image", aquí null simulado como ejemplo
            //.multiPart("image", "image.jpg", new ByteArrayInputStream(imageBytes), MediaType.IMAGE_JPEG_VALUE)
        .when()
            .post("/api/texts")
        .then()
            .statusCode(201)
            .body("titleEnglish", equalTo("Test Title."))
            .body("level", equalTo("HSK1"));
    }

    // Test E2E 3: POST /api/texts devuelve 403 forbidden cuando un usuario normal sube un texto con imagen opcional
    @Test
    @DisplayName("POST /api/texts should return 403 forbidden when a user uploads a text with optional image")
    public void testUploadTextWithUser() throws Exception {
        // Serializamos el TextDTO a JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String textJson = objectMapper.writeValueAsString(body); // body es tu JSONObject con los campos de TextDTO

        given()
            .cookies(userCookies) // cookies de usuario normal, no admin
            .contentType("multipart/form-data")
            // parte "data" con JSON
            .multiPart("data", "data.json", textJson.getBytes(), "application/json")
            // parte opcional "image", aquí null simulado como ejemplo
            //.multiPart("image", "image.jpg", new ByteArrayInputStream(imageBytes), MediaType.IMAGE_JPEG_VALUE)
        .when()
            .post("/api/texts")
        .then()
            .statusCode(403);
    }

    // Test E2E 4: GET /api/texts/level/HSK1 devuelve 200
    @Test
    @DisplayName("GET /api/texts/level/HSK1 should return 200 for any user")
    public void testGetTextsByLevel() {
        given()
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/texts/level/HSK1")
        .then()
            .statusCode(200);
    }

    // Test E2E 5: GET /api/texts/99999 con id inexistente devuelve 200 con cuerpo null
    @Test
    @DisplayName("GET /api/texts/99999 with non-existing id should return 200 for any user")
    public void testGetTextByNonExistingId() {
        given()
        .when()
            .get("/api/texts/99999")
        .then()
            .statusCode(200)
            .body(emptyOrNullString());
    }

    // Test E2E 6: POST /api/texts/validate sin autenticación devuelve 401
    @Test
    @DisplayName("POST /api/texts/validate without auth should return 401")
    public void testValidateTextUnauthenticated() {
        given()
            .param("chineseText", "你好。")
        .when()
            .post("/api/texts/validate")
        .then()
            .statusCode(401);
    }

    // Test E2E 7: POST /api/texts/validate devuelve 200 para un administrador autenticado
    @Test
    @DisplayName("POST /api/texts/validate with admin auth should return 200 ok")
    public void testValidateTextAuthenticated() {
        given()
            .cookies(adminCookies) // <--- así funcionan correctamente las cookies en RestAssured 6
            .param("chineseText", "你好。")
        .when()
            .post("/api/texts/validate")
        .then()
            .statusCode(200);
    }

    // Test E2E 8: POST /api/texts/validate devuelve 401 unauthorized si intenta validar cualquier texto un usuario sin autenticación
    @Test
    @DisplayName("POST /api/texts/validate without admin auth should return 401 unauthorized")
    public void testValidateTextNotAuthenticated() {
        given()
            .param("chineseText", "你好。")
        .when()
            .post("/api/texts/validate")
        .then()
            .statusCode(401);
    }

    // Test E2E 9: GET /api/users/me sin autenticación devuelve 401
    @Test
    @DisplayName("GET /api/users/me without auth should return 401")
    public void testGetMeUnauthenticated() {
        given()
        .when()
            .get("/api/users/me")
        .then()
            .statusCode(401);
    }

    // Test E2E 10: GET /api/users/me con autenticación devuelve el usuario
    @Test
    @DisplayName("GET /api/users/me with auth should return the user")
    public void testGetMeAuthenticated() {
        given()
            .cookies(adminCookies)
        .when()
            .get("/api/users/me")
        .then()
            .statusCode(200)
            .body("email", equalTo("admin@test.com"));
    }
}