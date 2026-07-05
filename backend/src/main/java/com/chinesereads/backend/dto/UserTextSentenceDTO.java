package com.chinesereads.backend.dto;

/** One sentence of a user's private text with its English/Spanish translation, aligned 1:1. */
public record UserTextSentenceDTO(
        String chinese,
        String english,
        String spanish) {
}
