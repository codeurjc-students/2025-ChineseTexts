package com.chinesereads.backend.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything the private reader needs to render a user's text, in a single
 * payload and derived purely from stored data (no global dictionary lookup):
 * the ordered word list (chinese + pinyin + per-word translations), the full
 * translations, and the ordered sentences with their translations aligned 1:1.
 */
public record UserTextReaderDTO(
        Long id,
        String title,
        String text,
        String englishTranslation,
        String spanishTranslation,
        LocalDate creationDate,
        List<UserTextWordDTO> words,
        List<UserTextSentenceDTO> sentences) {
}
