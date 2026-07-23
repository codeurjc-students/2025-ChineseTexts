package com.chinesereads.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record TextDTO(
    Long id,
    String titleEnglish,
    String titleSpanish,
    String text,
    String spanishTranslation,
    String englishTranslation,
    String level,
    // Topic tag keys from TextTopics.ALLOWED (nullable: pre-topics clients and
    // texts created before topics existed simply omit the field).
    List<String> topics,
    String englishDescription,
    String spanishDescription,
    LocalDate creationDate,
    // Aligned sentence pairs (nullable: texts created before they existed have
    // none and old clients may omit the field — never a primitive, Jackson 3
    // rejects bodies missing primitive fields).
    List<TextSentenceDTO> sentences) {
}
