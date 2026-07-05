package com.chinesereads.backend.Service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chinesereads.backend.Model.AppUsage;
import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.AppUsageRepository;
import com.chinesereads.backend.Repository.UserRepository;

/**
 * Enforces the two cost guards for AI/OCR text generation:
 *  - a per-user monthly quota, and
 *  - a global daily kill-switch across all users.
 *
 * {@link #reserveGeneration(User)} both checks AND increments the counters BEFORE
 * any paid API call, so the maximum daily API spend is bounded no matter how many
 * users or requests arrive. Limits are configurable in application.properties.
 */
@Service
public class UsageService {

    @Value("${usage.user.monthly-limit:30}")
    private int userMonthlyLimit;

    @Value("${usage.global.daily-limit:200}")
    private int globalDailyLimit;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppUsageRepository appUsageRepository;

    /**
     * Verifies both limits and, if allowed, consumes one unit from each counter.
     * Call this at the very start of a generation, before spending on OCR/AI.
     *
     * @throws UsageLimitException if the user's monthly quota or the global daily
     *                             cap has been reached.
     */
    @Transactional
    public void reserveGeneration(User user) {
        LocalDate today = LocalDate.now();

        // ——— Per-user monthly quota ———
        LocalDate monthStart = today.withDayOfMonth(1);
        if (user.getUsagePeriodStart() == null || !user.getUsagePeriodStart().equals(monthStart)) {
            user.setUsagePeriodStart(monthStart);
            user.setMonthlyTextCount(0);
        }
        if (user.getMonthlyTextCount() >= userMonthlyLimit) {
            throw new UsageLimitException(
                    "You've reached your monthly limit of " + userMonthlyLimit
                            + " text creations. It resets at the start of next month.");
        }

        // ——— Global daily kill-switch ———
        AppUsage usage = appUsageRepository.findByDay(today).orElseGet(() -> new AppUsage(today, 0));
        if (usage.getCount() >= globalDailyLimit) {
            throw new UsageLimitException(
                    "Text creation is temporarily unavailable for today. Please try again tomorrow.");
        }

        // ——— Consume one unit from each counter ———
        user.setMonthlyTextCount(user.getMonthlyTextCount() + 1);
        usage.setCount(usage.getCount() + 1);
        userRepository.save(user);
        appUsageRepository.save(usage);
    }

    public int getUserMonthlyLimit() {
        return userMonthlyLimit;
    }
}
