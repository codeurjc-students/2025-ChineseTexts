package com.chinesereads.backend.dto;

public record FounderSocialDTO(
    Long id,
    String label,
    String icon,
    String url,
    int displayOrder
) {}
