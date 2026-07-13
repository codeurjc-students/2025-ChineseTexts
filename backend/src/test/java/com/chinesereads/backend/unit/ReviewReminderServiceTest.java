package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.FlashcardRepository;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.EmailService;
import com.chinesereads.backend.Service.ReviewReminderService;

/**
 * Pins the four sending conditions of the daily review reminder (consent+due+
 * inactive+not-yet-reminded), the max-1/day idempotency, the lazy unsubscribe
 * token, and that a failed send is retried (not marked) without stopping the batch.
 */
@ExtendWith(MockitoExtension.class)
public class ReviewReminderServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 14);

    @Mock
    private UserRepository userRepository;

    @Mock
    private FlashcardRepository flashcardRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ReviewReminderService reminderService;

    private User user;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setEmail("ana@test.com");
        user.setName("Ana");
        user.setLanguage("es");
        user.setEmailConsent(true);
        user.setCurrentStreak(3);
    }

    @Test
    @DisplayName("Sends to a consenting, inactive user with due cards; marks the day and mints a token")
    public void testSendsAndMarks() {
        when(userRepository.findByEmailConsentTrueAndBlockedFalse()).thenReturn(List.of(user));
        when(flashcardRepository.countDue(user, TODAY)).thenReturn(5L);

        int sent = reminderService.remindUsersDue(TODAY);

        assertEquals(1, sent);
        verify(emailService).sendReviewReminderEmail(eq("ana@test.com"), eq("Ana"), eq("es"),
                eq(5L), eq(3), anyString());
        assertEquals(TODAY, user.getLastReviewReminderDay());
        assertNotNull(user.getUnsubscribeToken());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Skips a user who already read today (active users need no nudge)")
    public void testSkipsActiveToday() {
        user.setLastReadingDay(TODAY);
        when(userRepository.findByEmailConsentTrueAndBlockedFalse()).thenReturn(List.of(user));

        assertEquals(0, reminderService.remindUsersDue(TODAY));
        verify(emailService, never()).sendReviewReminderEmail(anyString(), anyString(), anyString(),
                anyLong(), anyInt(), anyString());
    }

    @Test
    @DisplayName("Skips a user with nothing due (no content, no email)")
    public void testSkipsNothingDue() {
        when(userRepository.findByEmailConsentTrueAndBlockedFalse()).thenReturn(List.of(user));
        when(flashcardRepository.countDue(user, TODAY)).thenReturn(0L);

        assertEquals(0, reminderService.remindUsersDue(TODAY));
        verify(emailService, never()).sendReviewReminderEmail(anyString(), anyString(), anyString(),
                anyLong(), anyInt(), anyString());
    }

    @Test
    @DisplayName("Never sends twice the same day (idempotent across job re-runs)")
    public void testMaxOncePerDay() {
        user.setLastReviewReminderDay(TODAY);
        when(userRepository.findByEmailConsentTrueAndBlockedFalse()).thenReturn(List.of(user));

        assertEquals(0, reminderService.remindUsersDue(TODAY));
        verify(emailService, never()).sendReviewReminderEmail(anyString(), anyString(), anyString(),
                anyLong(), anyInt(), anyString());
        verify(flashcardRepository, never()).countDue(any(), any());
    }

    @Test
    @DisplayName("A failed send is NOT marked as sent (retries next day) and never stops the batch")
    public void testFailureRetriesAndBatchContinues() {
        User second = new User();
        second.setEmail("luis@test.com");
        second.setName("Luis");
        second.setLanguage("en");
        second.setEmailConsent(true);

        when(userRepository.findByEmailConsentTrueAndBlockedFalse()).thenReturn(List.of(user, second));
        when(flashcardRepository.countDue(any(User.class), eq(TODAY))).thenReturn(2L);
        doThrow(new RuntimeException("brevo down"))
                .when(emailService).sendReviewReminderEmail(eq("ana@test.com"), anyString(), anyString(),
                        anyLong(), anyInt(), anyString());

        int sent = reminderService.remindUsersDue(TODAY);

        assertEquals(1, sent);                              // solo el segundo usuario
        assertNull(user.getLastReviewReminderDay());        // el fallido reintenta mañana
        assertEquals(TODAY, second.getLastReviewReminderDay());
        verify(userRepository, never()).save(user);
        verify(userRepository).save(second);
    }

    @Test
    @DisplayName("An existing unsubscribe token is reused, never rotated")
    public void testTokenIsStable() {
        user.setUnsubscribeToken("token-estable-123");
        when(userRepository.findByEmailConsentTrueAndBlockedFalse()).thenReturn(List.of(user));
        when(flashcardRepository.countDue(user, TODAY)).thenReturn(1L);

        reminderService.remindUsersDue(TODAY);

        assertEquals("token-estable-123", user.getUnsubscribeToken());
        verify(emailService).sendReviewReminderEmail(anyString(), anyString(), anyString(),
                anyLong(), anyInt(), eq("token-estable-123"));
    }
}
