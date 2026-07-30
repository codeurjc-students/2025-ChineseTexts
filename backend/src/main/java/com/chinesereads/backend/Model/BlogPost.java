package com.chinesereads.backend.Model;

import java.sql.Blob;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

/**
 * Entrada del blog (páginas públicas /blog y /blog/:slug): post estilo Medium
 * escrito por el admin con el editor Quill. Bilingüe con fallback (patrón de
 * los textos y del Hall of Fame): el frontend muestra el idioma activo y cae
 * al otro si está vacío.
 *
 * El HTML de contentEn/contentEs llega del editor y se guarda SIEMPRE saneado
 * (BlogService.sanitize, safelist jsoup) — nunca se persiste HTML crudo.
 * Un post solo es visible al público (y entra en /sitemap-blog.xml) cuando
 * published es true; los borradores solo los ve el admin.
 */
@Entity
public class BlogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // Identificador estable para la URL /blog/:slug.
    @Column(unique = true, nullable = false)
    private String slug;

    private String titleEn;

    private String titleEs;

    // Resumen corto: tarjeta del listado + meta description del post.
    @Column(columnDefinition = "TEXT")
    private String excerptEn;

    @Column(columnDefinition = "TEXT")
    private String excerptEs;

    // HTML saneado del cuerpo. @Lob String → LONGTEXT en MySQL / CLOB en H2
    // (portable en tests; columnDefinition="LONGTEXT" no compilaría en H2).
    @Lob
    private String contentEn;

    @Lob
    private String contentEs;

    // Imagen de portada: tarjeta del listado + og:image al compartir.
    @Lob
    private Blob cover;

    private boolean published;

    // Fecha de la PRIMERA publicación (no se re-fija al despublicar/republicar):
    // fecha visible del post y lastmod de respaldo en el sitemap.
    private LocalDate publishedOn;

    // Última modificación (cada save): dateModified del JSON-LD y lastmod.
    private LocalDateTime updatedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitleEn() {
        return titleEn;
    }

    public void setTitleEn(String titleEn) {
        this.titleEn = titleEn;
    }

    public String getTitleEs() {
        return titleEs;
    }

    public void setTitleEs(String titleEs) {
        this.titleEs = titleEs;
    }

    public String getExcerptEn() {
        return excerptEn;
    }

    public void setExcerptEn(String excerptEn) {
        this.excerptEn = excerptEn;
    }

    public String getExcerptEs() {
        return excerptEs;
    }

    public void setExcerptEs(String excerptEs) {
        this.excerptEs = excerptEs;
    }

    public String getContentEn() {
        return contentEn;
    }

    public void setContentEn(String contentEn) {
        this.contentEn = contentEn;
    }

    public String getContentEs() {
        return contentEs;
    }

    public void setContentEs(String contentEs) {
        this.contentEs = contentEs;
    }

    public Blob getCover() {
        return cover;
    }

    public void setCover(Blob cover) {
        this.cover = cover;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public LocalDate getPublishedOn() {
        return publishedOn;
    }

    public void setPublishedOn(LocalDate publishedOn) {
        this.publishedOn = publishedOn;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
