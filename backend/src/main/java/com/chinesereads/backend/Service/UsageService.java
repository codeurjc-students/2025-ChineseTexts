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

    // Cupo mensual ampliado para usuarios PREMIUM. Nunca es menor que el gratuito,
    // así que activar premium sólo puede subir el límite: nadie pierde acceso.
    @Value("${usage.premium.monthly-limit:100}")
    private int premiumMonthlyLimit;

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
        boolean isAdmin = user.getRoles() != null && user.getRoles().contains("ADMIN");
        // Tier of the monthly quota: PREMIUM subscribers get a higher ceiling than the
        // free plan. Admins are handled separately (fully exempt) below.
        int monthlyLimit = user.isPremiumActive() ? premiumMonthlyLimit : userMonthlyLimit;

        // ——— Per-user monthly quota (admins are exempt so the owner can test/demo
        // the product freely; the global kill-switch below still applies to everyone) ———
        if (!isAdmin) {
            LocalDate monthStart = today.withDayOfMonth(1);
            if (user.getUsagePeriodStart() == null || !user.getUsagePeriodStart().equals(monthStart)) {
                user.setUsagePeriodStart(monthStart);
                user.setMonthlyTextCount(0);
            }
            if (user.getMonthlyTextCount() >= monthlyLimit) {
                throw new UsageLimitException(
                        "You've reached your monthly limit of " + monthlyLimit
                                + " text creations. It resets at the start of next month.");
            }
        }

        // ——— Global daily kill-switch (applies to EVERYONE, admins included, as a
        // hard cost fuse) ———
        AppUsage usage = appUsageRepository.findByDay(today).orElseGet(() -> new AppUsage(today, 0));
        if (usage.getCount() >= globalDailyLimit) {
            throw new UsageLimitException(
                    "Text creation is temporarily unavailable for today. Please try again tomorrow.");
        }

        // ——— Consume one unit from each counter (admins don't spend monthly quota) ———
        if (!isAdmin) {
            user.setMonthlyTextCount(user.getMonthlyTextCount() + 1);
            userRepository.save(user);
        }
        usage.setCount(usage.getCount() + 1);
        appUsageRepository.save(usage);
    }

    /**
     * Lighter guard for the OCR "extract" step (Google Vision is much cheaper than
     * the DeepSeek generation): consumes one unit from the global daily counter only,
     * bounding total OCR spend, without touching the user's monthly quota (which is
     * spent when they actually generate the reader).
     */
    @Transactional
    public void reserveOcr() {
        LocalDate today = LocalDate.now();
        AppUsage usage = appUsageRepository.findByDay(today).orElseGet(() -> new AppUsage(today, 0));
        if (usage.getCount() >= globalDailyLimit) {
            throw new UsageLimitException(
                    "Text scanning is temporarily unavailable for today. Please try again tomorrow.");
        }
        usage.setCount(usage.getCount() + 1);
        appUsageRepository.save(usage);
    }

    public int getUserMonthlyLimit() {
        return userMonthlyLimit;
    }
}
