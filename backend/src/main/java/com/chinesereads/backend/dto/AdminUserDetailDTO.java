package com.chinesereads.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full user profile for the admin detail page: identity, status, activity dates
 * and derived statistics (collection / flashcard counts). Designed to be
 * extended later (e.g. OCR / text-generation usage counters from Task 3).
 */
public record AdminUserDetailDTO(
        Long id,
        String email,
        String name,
        String language,
        List<String> roles,
        boolean blocked,
        LocalDate registrationDate,
        LocalDateTime lastAccess,
        int collectionsCount,
        int flashcardsCount,
        List<AdminCollectionSummaryDTO> collections) {
}
