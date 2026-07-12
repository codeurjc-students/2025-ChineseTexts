package com.chinesereads.backend.dto;

public record FounderItemDTO(
    Long id,
    String heading,
    String subheading,
    String period,
    String location,
    String description,
    String linkUrl,
    String linkLabel,
    // Wrapper on purpose: Jackson 3 rejects request bodies that omit a primitive
    // field, and this DTO also arrives as a (possibly partial) @RequestBody payload.
    Integer displayOrder,
    Boolean hasLogo
) {}
