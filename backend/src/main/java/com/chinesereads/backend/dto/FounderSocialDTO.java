package com.chinesereads.backend.dto;

public record FounderSocialDTO(
    Long id,
    String label,
    String icon,
    String url,
    // Wrapper on purpose: Jackson 3 rejects request bodies that omit a primitive
    // field, and this DTO also arrives as a (possibly partial) @RequestBody payload.
    Integer displayOrder
) {}
