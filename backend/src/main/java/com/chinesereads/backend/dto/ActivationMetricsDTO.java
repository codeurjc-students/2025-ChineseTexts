package com.chinesereads.backend.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Activation metrics of the signup cohort registered between {@code from} and
 * {@code to} (inclusive): the campaign dashboard that tells whether new users
 * activate, come back and convert — computed entirely from the local database.
 *
 * <ul>
 *   <li>{@code signups} — accounts created in the range.</li>
 *   <li>{@code activatedDay1} — of those, users with reading activity on their
 *       registration day.</li>
 *   <li>{@code savedWord} — users who saved at least one flashcard.</li>
 *   <li>{@code measurableD7} — users whose day-7 window has fully elapsed; D7
 *       retention can only be judged on them.</li>
 *   <li>{@code retainedD7} — of the measurable ones, users who read again between
 *       days 1 and 7 after signup.</li>
 *   <li>{@code activePremium} — users currently holding an active premium.</li>
 *   <li>{@code bySource} — signups per acquisition source ({@code ?ref} code);
 *       {@code source} is null for organic/direct signups.</li>
 * </ul>
 */
public record ActivationMetricsDTO(
        LocalDate from,
        LocalDate to,
        long signups,
        long activatedDay1,
        long savedWord,
        long measurableD7,
        long retainedD7,
        long activePremium,
        List<SourceRow> bySource) {

    public record SourceRow(String source, long signups) {
    }
}
