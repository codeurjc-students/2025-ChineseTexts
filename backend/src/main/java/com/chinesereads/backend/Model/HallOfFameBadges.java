package com.chinesereads.backend.Model;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Controlled vocabulary of recognition badges for Hall of Fame entries.
 * Keys are stable English identifiers stored in the database; the user-facing
 * labels and descriptions live in the frontend i18n dictionaries (hofBadges.*),
 * so a badge renders as "Pionero" or "Pioneer" depending on the active
 * language without touching the data. A closed list (instead of free-form
 * badges) keeps the recognitions meaningful and consistent across entries.
 */
public final class HallOfFameBadges {

    // Display order matters to the frontend, which mirrors this list: keep it
    // in sync with HALL_OF_FAME_BADGES in frontend/src/app/data/hall-of-fame-badges.ts.
    public static final List<String> ALLOWED = List.of(
            "pioneer", "star", "prolific", "collaborator", "educator",
            "community");

    private static final Set<String> ALLOWED_SET = Set.copyOf(ALLOWED);

    private HallOfFameBadges() {
    }

    /**
     * Normalizes a client-provided badge list into a validated, de-duplicated,
     * canonically-ordered set. Throws IllegalArgumentException (mapped to 400
     * by the controller) on unknown keys.
     */
    public static Set<String> normalize(Collection<String> badges) {
        Set<String> result = new LinkedHashSet<>();
        if (badges == null) {
            return result;
        }
        for (String badge : badges) {
            if (badge == null || badge.isBlank()) {
                continue;
            }
            String key = badge.trim();
            if (!ALLOWED_SET.contains(key)) {
                throw new IllegalArgumentException("Unknown badge: " + key);
            }
            result.add(key);
        }
        // Canonical order so every API response lists badges consistently.
        Set<String> ordered = new LinkedHashSet<>();
        for (String key : ALLOWED) {
            if (result.contains(key)) {
                ordered.add(key);
            }
        }
        return ordered;
    }
}
