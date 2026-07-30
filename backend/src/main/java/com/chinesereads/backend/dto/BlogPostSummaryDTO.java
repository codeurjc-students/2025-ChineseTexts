package com.chinesereads.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Resumen para tarjetas del listado (/blog público y /blog/all admin): sin
 * cuerpos ni blob de portada — se construye desde la proyección
 * BlogPostRepository.BlogSummaryRow para no materializar Lobs.
 */
public record BlogPostSummaryDTO(
    Long id,
    String slug,
    String titleEn,
    String titleEs,
    String excerptEn,
    String excerptEs,
    Boolean hasCover,
    Boolean published,
    LocalDate publishedOn,
    LocalDateTime updatedAt
) {}
