package com.chinesereads.backend.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.chinesereads.backend.Model.HallOfFameEntry;
import com.chinesereads.backend.Repository.HallOfFameEntryRepository;

import io.restassured.RestAssured;

/**
 * E2E tests for the dynamic Hall of Fame sitemap (/sitemap-hall-of-fame.xml).
 * Like the texts and blog ones it lives OUTSIDE /api (sitemap path-scoping +
 * public document). The entity carries no modification date, so entries go
 * without &lt;lastmod&gt;.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SitemapHallOfFameApiTest {

    @LocalServerPort
    int port;

    @Autowired
    private HallOfFameEntryRepository entryRepository;

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = this.port;
        entryRepository.deleteAll();
    }

    @AfterEach
    public void tearDown() {
        entryRepository.deleteAll();
    }

    private HallOfFameEntry member(String slug, int order) {
        HallOfFameEntry entry = new HallOfFameEntry();
        entry.setName("Member " + slug);
        entry.setSlug(slug);
        entry.setDisplayOrder(order);
        return entryRepository.save(entry);
    }

    // Test E2E 1: lista cada miembro en ambos idiomas con sus hreflang
    @Test
    @DisplayName("GET /sitemap-hall-of-fame.xml is public and lists each member in both languages")
    public void listsMembers() {
        member("maria-lopez", 0);

        String xml = given()
        .when()
            .get("/sitemap-hall-of-fame.xml")
        .then()
            .statusCode(200)
            .header("Content-Type", containsString("xml"))
            .extract().asString();

        assertThat(xml, containsString("/hall-of-fame/maria-lopez</loc>"));
        assertThat(xml, containsString("/es/hall-of-fame/maria-lopez</loc>"));
        assertThat(xml, containsString("hreflang=\"en\""));
        assertThat(xml, containsString("hreflang=\"es\""));
        assertThat(xml, containsString("hreflang=\"x-default\""));
        // La entidad no tiene fecha de modificación: sin lastmod (válido).
        assertThat(xml, not(containsString("<lastmod>")));
    }

    // Test E2E 2: sin miembros devuelve un urlset vacío válido, nunca un error
    @Test
    @DisplayName("With no members it returns a valid empty urlset (never an error)")
    public void emptyUrlset() {
        given()
        .when()
            .get("/sitemap-hall-of-fame.xml")
        .then()
            .statusCode(200)
            .body(containsString("<urlset"))
            .body(not(containsString("<loc>")));
    }
}
