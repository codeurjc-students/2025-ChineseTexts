package com.chinesereads.backend.dto;

import java.time.LocalDate;

/** Compact view of a user's private text for the "My Tools" list. */
public record UserTextSummaryDTO(
        Long id,
        String title,
        LocalDate creationDate,
        int wordCount) {
}
