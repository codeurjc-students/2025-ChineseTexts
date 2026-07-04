package com.chinesereads.backend.dto;

import java.util.List;

public record FounderSectionDTO(
    Long id,
    String title,
    String type,
    int displayOrder,
    List<FounderItemDTO> items
) {}
