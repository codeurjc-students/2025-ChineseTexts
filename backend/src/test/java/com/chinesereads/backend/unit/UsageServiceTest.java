package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.chinesereads.backend.Model.AppUsage;
import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.AppUsageRepository;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.UsageLimitException;
import com.chinesereads.backend.Service.UsageService;

public class UsageServiceTest {

    private UserRepository userRepository;
    private AppUsageRepository appUsageRepository;
    private UsageService usageService;

    @BeforeEach
    public void setUp() {
        userRepository = mock(UserRepository.class);
        appUsageRepository = mock(AppUsageRepository.class);
        usageService = new UsageService(userRepository, appUsageRepository);

        injectField(usageService, "userMonthlyLimit", 2);
        injectField(usageService, "premiumDailyLimit", 5);
        injectField(usageService, "globalDailyLimit", 3);

        // By default there is no usage recorded for today.
        when(appUsageRepository.findByDay(any())).thenReturn(Optional.empty());
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

    private User userWithUsage(int monthlyCount, LocalDate periodStart) {
        User u = new User("u@u.com", "U", "pass", "en", "USER");
        u.setMonthlyTextCount(monthlyCount);
        u.setUsagePeriodStart(periodStart);
        return u;
    }

    private User premiumWithDaily(int dailyCount, LocalDate dayStart) {
        User u = new User("p@p.com", "P", "pass", "en", "USER");
        u.setPremiumUntil(LocalDateTime.now().plusDays(30)); // active subscription
        u.setDailyTextCount(dailyCount);
        u.setUsageDayStart(dayStart);
        return u;
    }

    @Test
    @DisplayName("Under the monthly limit: consumes one unit and does not throw")
    public void testUnderLimit() {
        User user = userWithUsage(0, LocalDate.now().withDayOfMonth(1));

        assertDoesNotThrow(() -> usageService.reserveGeneration(user));
        assertEquals(1, user.getMonthlyTextCount());
    }

    @Test
    @DisplayName("At the monthly limit: throws UsageLimitException")
    public void testMonthlyLimitReached() {
        User user = userWithUsage(2, LocalDate.now().withDayOfMonth(1)); // limit is 2

        assertThrows(UsageLimitException.class, () -> usageService.reserveGeneration(user));
    }

    @Test
    @DisplayName("A new month resets the monthly counter")
    public void testMonthlyResetOnNewMonth() {
        User user = userWithUsage(99, LocalDate.now().withDayOfMonth(1).minusMonths(1));

        assertDoesNotThrow(() -> usageService.reserveGeneration(user));
        // Reset to 0 for the new month, then incremented to 1.
        assertEquals(1, user.getMonthlyTextCount());
    }

    @Test
    @DisplayName("At the global daily cap: throws even if the user is under their quota")
    public void testGlobalDailyLimitReached() {
        User user = userWithUsage(0, LocalDate.now().withDayOfMonth(1));
        when(appUsageRepository.findByDay(any()))
                .thenReturn(Optional.of(new AppUsage(LocalDate.now(), 3))); // daily limit is 3

        assertThrows(UsageLimitException.class, () -> usageService.reserveGeneration(user));
    }

    @Test
    @DisplayName("Admins are exempt from the monthly limit and don't spend monthly quota")
    public void testAdminExemptFromMonthlyLimit() {
        User admin = new User("a@a.com", "A", "pass", "en", "USER", "ADMIN");
        admin.setMonthlyTextCount(99); // far over the limit of 2
        admin.setUsagePeriodStart(LocalDate.now().withDayOfMonth(1));

        assertDoesNotThrow(() -> usageService.reserveGeneration(admin));
        assertEquals(99, admin.getMonthlyTextCount()); // unchanged: no monthly quota spent
    }

    @Test
    @DisplayName("Admins are still bound by the global daily kill-switch")
    public void testAdminStillBoundByGlobalDaily() {
        User admin = new User("a@a.com", "A", "pass", "en", "USER", "ADMIN");
        when(appUsageRepository.findByDay(any()))
                .thenReturn(Optional.of(new AppUsage(LocalDate.now(), 3))); // daily limit is 3

        assertThrows(UsageLimitException.class, () -> usageService.reserveGeneration(admin));
    }

    @Test
    @DisplayName("Premium ignores the monthly limit (unlimited) and spends the daily counter, not the monthly one")
    public void testPremiumIgnoresMonthlyLimit() {
        User premium = userWithUsage(99, LocalDate.now().withDayOfMonth(1)); // far over the FREE limit of 2
        premium.setPremiumUntil(LocalDateTime.now().plusDays(30)); // active subscription

        assertDoesNotThrow(() -> usageService.reserveGeneration(premium));
        assertEquals(99, premium.getMonthlyTextCount()); // monthly quota untouched for premium
        assertEquals(1, premium.getDailyTextCount());    // premium spends the daily fair-use counter
    }

    @Test
    @DisplayName("Premium users are blocked once they reach the daily fair-use cap")
    public void testPremiumDailyCapReached() {
        User premium = premiumWithDaily(5, LocalDate.now()); // daily cap is 5

        assertThrows(UsageLimitException.class, () -> usageService.reserveGeneration(premium));
    }

    @Test
    @DisplayName("Premium is exempt from the global daily fuse")
    public void testPremiumExemptFromGlobalFuse() {
        User premium = premiumWithDaily(0, LocalDate.now());
        when(appUsageRepository.findByDay(any()))
                .thenReturn(Optional.of(new AppUsage(LocalDate.now(), 3))); // global fuse at its cap of 3

        // A free user would be blocked here, but premium ignores the global fuse entirely.
        assertDoesNotThrow(() -> usageService.reserveGeneration(premium));
        assertEquals(1, premium.getDailyTextCount());
    }

    @Test
    @DisplayName("A new day resets the premium daily counter")
    public void testPremiumDailyResetOnNewDay() {
        User premium = premiumWithDaily(99, LocalDate.now().minusDays(1)); // yesterday's usage

        assertDoesNotThrow(() -> usageService.reserveGeneration(premium));
        assertEquals(1, premium.getDailyTextCount()); // reset to 0 for the new day, then incremented
    }

    @Test
    @DisplayName("An expired premium grant falls back to the free plan limit")
    public void testExpiredPremiumUsesFreeLimit() {
        User expired = userWithUsage(2, LocalDate.now().withDayOfMonth(1)); // at the free limit
        expired.setPremiumUntil(LocalDateTime.now().minusDays(1)); // subscription already expired

        assertThrows(UsageLimitException.class, () -> usageService.reserveGeneration(expired));
    }

    @Test
    @DisplayName("getStatus reports the free plan's used count and limit")
    public void testStatusFree() {
        User user = userWithUsage(1, LocalDate.now().withDayOfMonth(1));

        var status = usageService.getStatus(user);

        assertEquals("free", status.plan());
        assertFalse(status.unlimited());
        assertEquals(1, status.used());
        assertEquals(2, status.limit()); // userMonthlyLimit injected as 2
    }

    @Test
    @DisplayName("getStatus reports 0 used when the stored month has rolled over")
    public void testStatusFreeRolledOverMonth() {
        User user = userWithUsage(99, LocalDate.now().withDayOfMonth(1).minusMonths(1));

        var status = usageService.getStatus(user);

        assertEquals(0, status.used()); // last month's count does not carry over
        assertEquals(2, status.limit());
    }

    @Test
    @DisplayName("getStatus reports premium as unlimited")
    public void testStatusPremium() {
        User premium = userWithUsage(0, LocalDate.now().withDayOfMonth(1));
        premium.setPremiumUntil(LocalDateTime.now().plusDays(10));

        var status = usageService.getStatus(premium);

        assertEquals("premium", status.plan());
        assertTrue(status.unlimited());
    }

    @Test
    @DisplayName("getStatus reports admin as unlimited")
    public void testStatusAdmin() {
        User admin = new User("a@a.com", "A", "pass", "en", "USER", "ADMIN");

        var status = usageService.getStatus(admin);

        assertEquals("admin", status.plan());
        assertTrue(status.unlimited());
    }
}
