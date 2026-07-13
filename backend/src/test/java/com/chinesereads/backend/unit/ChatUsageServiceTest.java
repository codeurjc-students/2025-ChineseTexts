package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.chinesereads.backend.Service.ChatUsageService;
import com.chinesereads.backend.Service.UsageLimitException;

/**
 * Tests the per-user monthly AI chat quota: free users are capped, premium and admins
 * are unlimited, and the counter resets each month.
 */
public class ChatUsageServiceTest {

    private UserRepository userRepository;
    private ChatUsageService chatUsageService;

    @BeforeEach
    public void setUp() {
        userRepository = mock(UserRepository.class);
        chatUsageService = new ChatUsageService(userRepository);
        injectField(chatUsageService, "chatMonthlyLimit", 3);
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
        u.setChatUsagePeriodStart(LocalDate.now().withDayOfMonth(1));
        return u;
    }

    @Test
    @DisplayName("A free user under the limit consumes one message")
    public void testUnderLimit() {
        User user = freeUser();
        assertDoesNotThrow(() -> chatUsageService.reserveChat(user));
        assertEquals(1, user.getMonthlyChatCount());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("A free user at the limit is blocked")
    public void testLimitReached() {
        User user = freeUser();
        user.setMonthlyChatCount(3); // limit is 3
        assertThrows(UsageLimitException.class, () -> chatUsageService.reserveChat(user));
    }

    @Test
    @DisplayName("Premium users are unlimited (no counter spent)")
    public void testPremiumUnlimited() {
        User premium = freeUser();
        premium.setPremiumUntil(LocalDateTime.now().plusDays(10));
        premium.setMonthlyChatCount(999);

        assertTrue(chatUsageService.isUnlimited(premium));
        assertDoesNotThrow(() -> chatUsageService.reserveChat(premium));
        assertEquals(999, premium.getMonthlyChatCount()); // untouched
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admins are unlimited")
    public void testAdminUnlimited() {
        User admin = new User("a@a.com", "A", "pass", "en", "USER", "ADMIN");
        assertTrue(chatUsageService.isUnlimited(admin));
        assertDoesNotThrow(() -> chatUsageService.reserveChat(admin));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("A free user is not unlimited")
    public void testFreeNotUnlimited() {
        assertFalse(chatUsageService.isUnlimited(freeUser()));
    }

    @Test
    @DisplayName("A new month resets the chat counter")
    public void testMonthlyReset() {
        User user = freeUser();
        user.setChatUsagePeriodStart(LocalDate.now().withDayOfMonth(1).minusMonths(1));
        user.setMonthlyChatCount(99);

        assertDoesNotThrow(() -> chatUsageService.reserveChat(user));
        assertEquals(1, user.getMonthlyChatCount()); // reset then incremented
    }
}
