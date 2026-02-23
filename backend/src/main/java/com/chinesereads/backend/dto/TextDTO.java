package com.chinesereads.backend.dto;

import java.time.LocalDate;

public record TextDTO(
    Long id,
    String titleEnglish,
    String titleSpanish,
    String text,
    String spanishTranslation,
    String englishTranslation,
    String level,
    String englishDescription,
    String spanishDescription,
    LocalDate creationDate) {
}
