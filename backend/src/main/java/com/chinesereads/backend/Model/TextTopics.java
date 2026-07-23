package com.chinesereads.backend.Model;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Controlled vocabulary of topic tags for public texts. Keys are stable
 * English identifiers stored in the database; the user-facing labels live in
 * the frontend i18n dictionaries (topics.*), so a tag renders as "Deportes"
 * or "Sports" depending on the active language without touching the data.
 * A closed list (instead of free-form tags) keeps the filter UI meaningful
 * and avoids near-duplicate tags ("sport"/"sports"/"deporte") accumulating.
 */
public final class TextTopics {

    public static final int MAX_PER_TEXT = 5;

    // Display order matters to the frontend, which mirrors this list: keep it
    // in sync with TEXT_TOPICS in frontend/src/app/services/texts.service.ts.
    public static final List<String> ALLOWED = List.of(
            "daily-life", "family", "food", "travel", "culture", "history",
            "school", "work", "sports", "games", "nature", "animals",
            "science", "technology", "health", "society", "politics",
            "entertainment");

    private static final Set<String> ALLOWED_SET = Set.copyOf(ALLOWED);

    private TextTopics() {
    }

    /**
     * Normalizes a client-provided topic list into a validated, de-duplicated,
     * canonically-ordered set. Throws IllegalArgumentException (mapped to 400
     * by the controllers) on unknown keys or too many topics.
     */
    public static Set<String> normalize(Collection<String> topics) {
        Set<String> result = new LinkedHashSet<>();
        if (topics == null) {
            return result;
        }
        for (String topic : topics) {
            if (topic == null || topic.isBlank()) {
                continue;
            }
            String key = topic.trim();
            if (!ALLOWED_SET.contains(key)) {
                throw new IllegalArgumentException("Unknown topic: " + key);
            }
            result.add(key);
        }
        if (result.size() > MAX_PER_TEXT) {
            throw new IllegalArgumentException(
                    "A text can have at most " + MAX_PER_TEXT + " topics.");
        }
        // Canonical order so every API response lists topics consistently.
        Set<String> ordered = new LinkedHashSet<>();
        for (String key : ALLOWED) {
            if (result.contains(key)) {
                ordered.add(key);
            }
        }
        return ordered;
    }
}
