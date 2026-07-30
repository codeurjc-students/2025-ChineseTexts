package com.chinesereads.backend.dto;

/**
 * Payload de creación/edición desde el editor admin. Update parcial: un campo
 * null se deja como está (por eso todos los campos son wrappers/objetos —
 * Jackson 3 rechaza bodies parciales que omitan un primitivo). El HTML de
 * contentEn/contentEs se sanea SIEMPRE en BlogService antes de persistir.
 */
public record BlogPostUpsertDTO(
    String slug,
    String titleEn,
    String titleEs,
    String excerptEn,
    String excerptEs,
    String contentEn,
    String contentEs,
    Boolean published
) {}
