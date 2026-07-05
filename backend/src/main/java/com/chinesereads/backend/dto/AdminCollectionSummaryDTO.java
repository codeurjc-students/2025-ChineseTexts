package com.chinesereads.backend.dto;

/**
 * Lightweight view of one of a user's collections, shown on the admin user
 * detail page (title + how many flashcards it holds).
 */
public record AdminCollectionSummaryDTO(
        Long id,
        String title,
        int flashcardsCount) {
}
