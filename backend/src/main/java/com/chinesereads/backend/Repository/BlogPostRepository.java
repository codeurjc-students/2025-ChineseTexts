package com.chinesereads.backend.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.chinesereads.backend.Model.BlogPost;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    /**
     * Fila resumen para listados y tarjetas: todo menos los cuerpos y el blob
     * de portada (hasCover se calcula en SQL), para no materializar Lobs.
     */
    interface BlogSummaryRow {
        Long getId();
        String getSlug();
        String getTitleEn();
        String getTitleEs();
        String getExcerptEn();
        String getExcerptEs();
        Boolean getHasCover();
        Boolean getPublished();
        LocalDate getPublishedOn();
        LocalDateTime getUpdatedAt();
    }

    /** Fila mínima para /sitemap-blog.xml (solo publicados, sin Lobs). */
    interface BlogSitemapRow {
        String getSlug();
        LocalDate getPublishedOn();
        LocalDateTime getUpdatedAt();
    }

    Optional<BlogPost> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("""
            SELECT p.id AS id, p.slug AS slug, p.titleEn AS titleEn, p.titleEs AS titleEs,
                   p.excerptEn AS excerptEn, p.excerptEs AS excerptEs,
                   (p.cover IS NOT NULL) AS hasCover, p.published AS published,
                   p.publishedOn AS publishedOn, p.updatedAt AS updatedAt
            FROM BlogPost p WHERE p.published = true
            ORDER BY p.publishedOn DESC, p.id DESC""")
    List<BlogSummaryRow> findPublishedSummaries();

    @Query("""
            SELECT p.id AS id, p.slug AS slug, p.titleEn AS titleEn, p.titleEs AS titleEs,
                   p.excerptEn AS excerptEn, p.excerptEs AS excerptEs,
                   (p.cover IS NOT NULL) AS hasCover, p.published AS published,
                   p.publishedOn AS publishedOn, p.updatedAt AS updatedAt
            FROM BlogPost p
            ORDER BY p.updatedAt DESC, p.id DESC""")
    List<BlogSummaryRow> findAllSummaries();

    @Query("""
            SELECT p.slug AS slug, p.publishedOn AS publishedOn, p.updatedAt AS updatedAt
            FROM BlogPost p WHERE p.published = true ORDER BY p.id""")
    List<BlogSitemapRow> findSitemapRows();
}
