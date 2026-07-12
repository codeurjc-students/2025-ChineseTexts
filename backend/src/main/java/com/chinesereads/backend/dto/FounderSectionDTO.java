package com.chinesereads.backend.dto;

import java.util.List;

public record FounderSectionDTO(
    Long id,
    String title,
    String type,
    // Wrapper on purpose: Jackson 3 rejects request bodies that omit a primitive
    // field, and this DTO also arrives as a (possibly partial) @RequestBody payload.
    Integer displayOrder,
    List<FounderItemDTO> items
) {}
