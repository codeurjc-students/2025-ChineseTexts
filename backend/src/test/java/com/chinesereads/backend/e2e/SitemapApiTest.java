package com.chinesereads.backend.e2e;

import static io.restassured.RestAssured.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.chinesereads.backend.Model.Text;
import com.chinesereads.backend.Repository.TextRepository;

import io.restassured.RestAssured;

/**
 * E2E tests for the dynamic texts sitemap (/sitemap-texts.xml). It lives OUTSIDE
 * /api on purpose (sitemap protocol path-scoping + public document), so these
 * tests also pin that it stays reachable without authentication.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SitemapApiTest {

    @LocalServerPort
    int port;

    @Autowired
    private TextRepository textRepository;

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = this.port;
        textRepository.deleteAll();
    }

    @AfterEach
    public void tearDown() {
        textRepository.deleteAll();
    }

    // Test E2E 1: el sitemap lista cada texto en ambos idiomas, con alternates y lastmod
    @Test
    @DisplayName("GET /sitemap-texts.xml should be public and list every text in both languages")
    public void testSitemapListsTexts() {
        Text text = new Text("Sitemap Title.", "Título Sitemap.", "你好。", "Hello.", "Hola.",
                "Description.", "Descripción.", "HSK1", LocalDate.of(2026, 1, 1), null);
        long id = textRepository.save(text).getId();

        String xml = given()
        .when()
            .get("/sitemap-texts.xml")
        .then()
            .statusCode(200)
            .header("Content-Type", containsString("xml"))
            .extract().asString();

        // Entrada inglesa y gemela española, con sus alternates hreflang y lastmod.
        assertThat(xml, containsString("<loc>"));
        assertThat(xml, containsString("/text/" + id + "</loc>"));
        assertThat(xml, containsString("/es/text/" + id + "</loc>"));
        assertThat(xml, containsString("hreflang=\"en\""));
        assertThat(xml, containsString("hreflang=\"es\""));
        assertThat(xml, containsString("hreflang=\"x-default\""));
        assertThat(xml, containsString("<lastmod>2026-01-01</lastmod>"));
    }

    // Test E2E 2: sin textos devuelve un urlset válido y vacío (nunca un error)
    @Test
    @DisplayName("GET /sitemap-texts.xml with no texts should return a valid empty urlset")
    public void testSitemapEmpty() {
        given()
        .when()
            .get("/sitemap-texts.xml")
        .then()
            .statusCode(200)
            .body(containsString("<urlset"))
            .body(not(containsString("<loc>")));
    }
}
