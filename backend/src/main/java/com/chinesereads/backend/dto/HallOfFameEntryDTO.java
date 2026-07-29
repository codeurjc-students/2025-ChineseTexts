package com.chinesereads.backend.dto;

import java.util.List;

public record HallOfFameEntryDTO(
    Long id,
    String name,
    String slug,
    String tagline,
    String bioEn,
    String bioEs,
    String discountCode,
    // Wrappers on purpose: Jackson 3 rejects request bodies that omit a primitive
    // field, and this DTO also arrives as a (possibly partial) @RequestBody payload.
    Integer displayOrder,
    Boolean hasPhoto,
    List<String> badges,
    List<HallOfFameSocialDTO> socials
) {}
