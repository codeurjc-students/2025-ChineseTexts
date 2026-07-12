package com.chinesereads.backend.dto;

import java.time.LocalDate;

/** Outcome of grading one flashcard: when the card will be shown again. */
public record SrsReviewResultDTO(
    long flashcardId,
    int intervalDays,
    LocalDate nextDue) {
}
