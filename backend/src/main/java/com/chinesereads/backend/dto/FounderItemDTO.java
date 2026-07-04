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
    int displayOrder,
    boolean hasLogo
) {}
