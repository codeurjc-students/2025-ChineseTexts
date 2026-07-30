package com.chinesereads.backend.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.chinesereads.backend.Model.BlogPost;
import com.chinesereads.backend.Repository.BlogPostRepository;

import io.restassured.RestAssured;

/**
 * E2E tests for the dynamic blog sitemap (/sitemap-blog.xml). Like the texts
 * one it lives OUTSIDE /api (sitemap path-scoping + public document) and must
 * list ONLY published posts — drafts stay invisible to crawlers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SitemapBlogApiTest {

    @LocalServerPort
    int port;

    @Autowired
    private BlogPostRepository postRepository;

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = this.port;
        postRepository.deleteAll();
    }

    @AfterEach
    public void tearDown() {
        postRepository.deleteAll();
    }

    private BlogPost post(String slug, boolean published) {
        BlogPost post = new BlogPost();
        post.setSlug(slug);
        post.setTitleEn("Title " + slug);
        post.setPublished(published);
        if (published) {
            post.setPublishedOn(LocalDate.of(2026, 7, 1));
        }
        post.setUpdatedAt(LocalDateTime.of(2026, 7, 15, 12, 0));
        return postRepository.save(post);
    }

    @Test
    @DisplayName("GET /sitemap-blog.xml is public and lists each published post in both languages")
    public void listsPublishedPosts() {
        post("learn-hsk1-fast", true);

        String xml = given()
        .when()
            .get("/sitemap-blog.xml")
        .then()
            .statusCode(200)
            .header("Content-Type", containsString("xml"))
            .extract().asString();

        assertThat(xml, containsString("/blog/learn-hsk1-fast</loc>"));
        assertThat(xml, containsString("/es/blog/learn-hsk1-fast</loc>"));
        assertThat(xml, containsString("hreflang=\"en\""));
        assertThat(xml, containsString("hreflang=\"es\""));
        assertThat(xml, containsString("hreflang=\"x-default\""));
        // lastmod = updatedAt (más reciente que publishedOn).
        assertThat(xml, containsString("<lastmod>2026-07-15</lastmod>"));
    }

    @Test
    @DisplayName("Drafts are excluded from the sitemap")
    public void draftsAreExcluded() {
        post("published-post", true);
        post("secret-draft", false);

        String xml = given().when().get("/sitemap-blog.xml")
                .then().statusCode(200).extract().asString();

        assertThat(xml, containsString("/blog/published-post</loc>"));
        assertThat(xml, not(containsString("secret-draft")));
    }

    @Test
    @DisplayName("With no posts it returns a valid empty urlset (never an error)")
    public void emptyUrlset() {
        given()
        .when()
            .get("/sitemap-blog.xml")
        .then()
            .statusCode(200)
            .body(containsString("<urlset"))
            .body(not(containsString("<loc>")));
    }
}
