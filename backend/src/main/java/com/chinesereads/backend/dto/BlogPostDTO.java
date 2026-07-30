package com.chinesereads.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Post completo (detalle público por slug + editor admin por id). La portada
 * (Blob) no se serializa: se sirve en su endpoint propio (`/{id}/cover`) y el
 * DTO lleva `hasCover` para que el frontend sepa si pedirla.
 */
public record BlogPostDTO(
    Long id,
    String slug,
    String titleEn,
    String titleEs,
    String excerptEn,
    String excerptEs,
    String contentEn,
    String contentEs,
    // Wrappers on purpose: Jackson 3 rejects request bodies that omit a primitive
    // field, and record shapes stay uniform across the blog DTOs.
    Boolean hasCover,
    Boolean published,
    LocalDate publishedOn,
    LocalDateTime updatedAt
) {}
