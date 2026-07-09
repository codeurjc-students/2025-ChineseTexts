package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.AudioUsageService;
import com.chinesereads.backend.Service.UsageLimitException;

/**
 * Tests the per-user monthly audio quota in isolation. Word plays and sentence/text
 * plays are metered in separate buckets; premium and admins are exempt.
 */
public class AudioUsageServiceTest {

    private UserRepository userRepository;
    private AudioUsageService audioUsageService;

    @BeforeEach
    public void setUp() {
        userRepository = mock(UserRepository.class);
        audioUsageService = new AudioUsageService();
        injectField(audioUsageService, "userRepository", userRepository);
        injectField(audioUsageService, "wordMonthlyLimit", 3);
        injectField(audioUsageService, "phraseMonthlyLimit", 2);
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private User freeUser() {
        User u = new User("u@u.com", "U", "pass", "en", "USER");
        u.setAudioUsagePeriodStart(LocalDate.now().withDayOfMonth(1));
        return u;
    }

    @Test
    @DisplayName("Word play under the limit consumes the word counter only")
    public void testWordUnderLimit() {
        User user = freeUser();
        assertDoesNotThrow(() -> audioUsageService.reserveAudio(user, true));
        assertEquals(1, user.getMonthlyWordAudioCount());
        assertEquals(0, user.getMonthlyPhraseAudioCount());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Word play at the limit throws")
    public void testWordLimitReached() {
        User user = freeUser();
        user.setMonthlyWordAudioCount(3); // word limit is 3
        assertThrows(UsageLimitException.class, () -> audioUsageService.reserveAudio(user, true));
    }

    @Test
    @DisplayName("Sentence/text play under the limit consumes the phrase counter only")
    public void testPhraseUnderLimit() {
        User user = freeUser();
        assertDoesNotThrow(() -> audioUsageService.reserveAudio(user, false));
        assertEquals(1, user.getMonthlyPhraseAudioCount());
        assertEquals(0, user.getMonthlyWordAudioCount());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Sentence/text play at the limit throws")
    public void testPhraseLimitReached() {
        User user = freeUser();
        user.setMonthlyPhraseAudioCount(2); // phrase limit is 2
        assertThrows(UsageLimitException.class, () -> audioUsageService.reserveAudio(user, false));
    }

    @Test
    @DisplayName("The two buckets are independent: a full word bucket doesn't block phrases")
    public void testBucketsAreIndependent() {
        User user = freeUser();
        user.setMonthlyWordAudioCount(3); // word bucket full
        assertDoesNotThrow(() -> audioUsageService.reserveAudio(user, false)); // phrase still allowed
        assertEquals(1, user.getMonthlyPhraseAudioCount());
    }

    @Test
    @DisplayName("Premium users are exempt (unlimited, no counter spent)")
    public void testPremiumExempt() {
        User premium = freeUser();
        premium.setPremiumUntil(LocalDateTime.now().plusDays(30));
        premium.setMonthlyWordAudioCount(999);
        assertDoesNotThrow(() -> audioUsageService.reserveAudio(premium, true));
        assertEquals(999, premium.getMonthlyWordAudioCount()); // untouched
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admins are exempt (unlimited, no counter spent)")
    public void testAdminExempt() {
        User admin = new User("a@a.com", "A", "pass", "en", "USER", "ADMIN");
        admin.setMonthlyPhraseAudioCount(999);
        assertDoesNotThrow(() -> audioUsageService.reserveAudio(admin, false));
        assertEquals(999, admin.getMonthlyPhraseAudioCount());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("A new month resets both audio counters")
    public void testMonthlyReset() {
        User user = freeUser();
        user.setAudioUsagePeriodStart(LocalDate.now().withDayOfMonth(1).minusMonths(1));
        user.setMonthlyWordAudioCount(99);
        user.setMonthlyPhraseAudioCount(99);

        assertDoesNotThrow(() -> audioUsageService.reserveAudio(user, true));
        assertEquals(1, user.getMonthlyWordAudioCount());  // reset then incremented
        assertEquals(0, user.getMonthlyPhraseAudioCount()); // reset
    }
}
