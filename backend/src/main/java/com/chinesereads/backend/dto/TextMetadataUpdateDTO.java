package com.chinesereads.backend.dto;

import java.util.List;

/**
 * Partial metadata update for a public text (PATCH /api/texts/{id}, ADMIN).
 * Every field is nullable: null means "leave unchanged". Deliberately excludes
 * the Chinese body, translations and sentences — editing those would require
 * re-validating the aligned sentence pairs, so content changes still go
 * through delete + re-upload where that validation lives.
 */
public record TextMetadataUpdateDTO(
    String titleEnglish,
    String titleSpanish,
    String englishDescription,
    String spanishDescription,
    String level,
    // Full replacement when present (an empty list clears all topics).
    List<String> topics) {
}
