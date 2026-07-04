package com.chinesereads.backend.dto;

import java.util.List;

public record FounderProfileDTO(
    Long id,
    String name,
    String role,
    String tagline,
    String location,
    String summary,
    boolean hasPhoto,
    List<FounderSocialDTO> socials,
    List<FounderSectionDTO> sections
) {}
