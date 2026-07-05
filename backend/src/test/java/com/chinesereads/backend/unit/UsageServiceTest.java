package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
        usageService = new UsageService();

        injectField(usageService, "userRepository", userRepository);
        injectField(usageService, "appUsageRepository", appUsageRepository);
        injectField(usageService, "userMonthlyLimit", 2);
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
}
