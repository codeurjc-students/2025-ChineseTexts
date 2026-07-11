package com.chinesereads.backend.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * The user's progress snapshot for the stats page and the header streak flame.
 *
 * @param currentStreak consecutive days with at least one reading (0 if broken)
 * @param bestStreak    longest streak ever achieved
 * @param readToday     true once the user has read something today (keeps the flame lit)
 * @param textsRead     distinct text-days read in total
 * @param wordsSaved    flashcards saved across all collections
 * @param week          last 7 days (oldest first) with the texts read each day
 */
public record StatsDTO(
        int currentStreak,
        int bestStreak,
        boolean readToday,
        long textsRead,
        int wordsSaved,
        List<DayCountDTO> week) {

    /** Activity of a single day, for the weekly chart. */
    public record DayCountDTO(LocalDate day, int count) {
    }
}
