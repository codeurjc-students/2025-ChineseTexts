package com.chinesereads.backend.dto;

import java.util.List;

public record FounderProfileDTO(
    Long id,
    String name,
    String role,
    String tagline,
    String location,
    String summary,
    // Wrapper on purpose: Jackson 3 rejects request bodies that omit a primitive
    // field, and this DTO also arrives as a (possibly partial) @RequestBody payload.
    Boolean hasPhoto,
    List<FounderSocialDTO> socials,
    List<FounderSectionDTO> sections
) {}
