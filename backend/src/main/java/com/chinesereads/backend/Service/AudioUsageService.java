package com.chinesereads.backend.Service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;

/**
 * Enforces per-user monthly audio (TTS) quotas for the FREE plan. Audio calls the
 * paid Google Cloud TTS API, so it is registered-only and metered in two buckets that
 * cost very differently: single-word playback (cheap, the core "tap a word to hear it"
 * interaction) gets a high monthly quota, while sentence / full-text narration
 * (expensive) gets a small one — the latter being the real premium draw.
 *
 * <p>Active PREMIUM subscribers and admins are exempt (unlimited). Limits are
 * configurable in application.properties.
 */
@Service
public class AudioUsageService {

    @Value("${usage.audio.word.monthly-limit:100}")
    private int wordMonthlyLimit;

    @Value("${usage.audio.phrase.monthly-limit:15}")
    private int phraseMonthlyLimit;

    private final UserRepository userRepository;

    public AudioUsageService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Verifies the relevant monthly audio quota and, if allowed, consumes one unit.
     * Call before synthesizing. Admins and active premium users are exempt.
     *
     * @param isWord true for single-word playback, false for sentence / full-text.
     * @throws UsageLimitException if the user's monthly audio quota is reached.
     */
    @Transactional
    public void reserveAudio(User user, boolean isWord) {
        boolean isAdmin = user.getRoles() != null && user.getRoles().contains("ADMIN");
        if (isAdmin || user.isPremiumActive()) {
            return; // premium/admin: unlimited audio
        }

        // Reset both counters at the start of a new month.
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        if (user.getAudioUsagePeriodStart() == null
                || !user.getAudioUsagePeriodStart().equals(monthStart)) {
            user.setAudioUsagePeriodStart(monthStart);
            user.setMonthlyWordAudioCount(0);
            user.setMonthlyPhraseAudioCount(0);
        }

        if (isWord) {
            if (user.getMonthlyWordAudioCount() >= wordMonthlyLimit) {
                throw new UsageLimitException(
                        "You've reached your free monthly limit of " + wordMonthlyLimit
                                + " word audio plays. Upgrade to Premium for unlimited audio.");
            }
            user.setMonthlyWordAudioCount(user.getMonthlyWordAudioCount() + 1);
        } else {
            if (user.getMonthlyPhraseAudioCount() >= phraseMonthlyLimit) {
                throw new UsageLimitException(
                        "You've reached your free monthly limit of " + phraseMonthlyLimit
                                + " sentence and full-text audio plays. Upgrade to Premium for unlimited audio.");
            }
            user.setMonthlyPhraseAudioCount(user.getMonthlyPhraseAudioCount() + 1);
        }
        userRepository.save(user);
    }
}
