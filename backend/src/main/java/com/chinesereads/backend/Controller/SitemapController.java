package com.chinesereads.backend.Controller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chinesereads.backend.Repository.BlogPostRepository;
import com.chinesereads.backend.Repository.BlogPostRepository.BlogSitemapRow;
import com.chinesereads.backend.Repository.TextRepository.TextSitemapRow;
import com.chinesereads.backend.Service.TextService;

/**
 * Dynamic sitemap for the individual text pages (/text/:id and /es/text/:id).
 *
 * The main sitemap.xml is a static frontend asset that can only list the fixed
 * public routes; texts live in the database and grow weekly, so their sitemap
 * has to be generated here on demand. Caddy proxies /sitemap-texts.xml to the
 * backend and robots.txt references it, so crawlers index every text directly
 * instead of relying only on internal-link discovery from the HSK listings.
 *
 * The path is deliberately OUTSIDE /api: the sitemap protocol scopes a sitemap
 * to its own directory and below, so it must live at the site root to be able
 * to list /text/... URLs. Being outside /api also leaves it outside the JWT
 * security chain (SecurityConfig matches /api/** only) — it is a public document.
 */
@RestController
public class SitemapController {

    private final TextService textService;
    private final BlogPostRepository blogPostRepository;

    // Site origin for absolute URLs. Same property Stripe uses for its return
    // URLs; in production docker-compose sets it to https://chinesereads.com.
    @Value("${app.public-url}")
    private String publicUrl;

    public SitemapController(TextService textService, BlogPostRepository blogPostRepository) {
        this.textService = textService;
        this.blogPostRepository = blogPostRepository;
    }

    @GetMapping(value = "/sitemap-texts.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String textsSitemap() {
        String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"\n");
        xml.append("        xmlns:xhtml=\"http://www.w3.org/1999/xhtml\">\n");
        for (TextSitemapRow row : textService.getSitemapRows()) {
            String en = base + "/text/" + row.getId();
            String es = base + "/es/text/" + row.getId();
            appendUrl(xml, en, en, es, row.getCreationDate());
            appendUrl(xml, es, en, es, row.getCreationDate());
        }
        xml.append("</urlset>\n");
        return xml.toString();
    }

    /**
     * Dynamic sitemap for the published blog posts (/blog/:slug and its /es
     * twin) — same rationale and format as /sitemap-texts.xml: posts live in
     * the database and must be indexable without a redeploy. Drafts are
     * excluded at the query level. Slugs are [a-z0-9-] by construction
     * (BlogService normalizes them), so the no-escaping invariant holds.
     */
    @GetMapping(value = "/sitemap-blog.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String blogSitemap() {
        String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"\n");
        xml.append("        xmlns:xhtml=\"http://www.w3.org/1999/xhtml\">\n");
        for (BlogSitemapRow row : blogPostRepository.findSitemapRows()) {
            String en = base + "/blog/" + row.getSlug();
            String es = base + "/es/blog/" + row.getSlug();
            LocalDate lastmod = row.getUpdatedAt() != null
                    ? row.getUpdatedAt().toLocalDate()
                    : row.getPublishedOn();
            appendUrl(xml, en, en, es, lastmod);
            appendUrl(xml, es, en, es, lastmod);
        }
        xml.append("</urlset>\n");
        return xml.toString();
    }

    /**
     * One &lt;url&gt; entry with its hreflang alternates (en, es, x-default→en),
     * mirroring the exact format of the static sitemap.xml so Google treats both
     * files consistently. Only ids and dates go into the XML — no user content,
     * so no escaping is needed.
     */
    private void appendUrl(StringBuilder xml, String loc, String en, String es, LocalDate lastmod) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(loc).append("</loc>\n");
        xml.append("    <xhtml:link rel=\"alternate\" hreflang=\"en\" href=\"").append(en).append("\"/>\n");
        xml.append("    <xhtml:link rel=\"alternate\" hreflang=\"es\" href=\"").append(es).append("\"/>\n");
        xml.append("    <xhtml:link rel=\"alternate\" hreflang=\"x-default\" href=\"").append(en).append("\"/>\n");
        if (lastmod != null) {
            xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        }
        xml.append("  </url>\n");
    }
}
