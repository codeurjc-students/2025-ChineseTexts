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
 * Enforces the cost guards for AI/OCR text generation, by tier:
 *  - FREE users: a per-user monthly quota AND a global daily kill-switch.
 *  - PREMIUM users: no monthly limit ("unlimited"), exempt from the global fuse, and
 *    bounded only by a high per-user daily fair-use cap (so a single abusive account
 *    can't run up the bill). Their usage never touches the global counter, so it can't
 *    starve free users of the shared daily budget — and the shared budget can't tell a
 *    paying customer to "come back tomorrow".
 *  - ADMINS: exempt from personal quotas, but still subject to the global fuse.
 *
 * {@link #reserveGeneration(User)} both checks AND increments the counters BEFORE
 * any paid API call, so the maximum daily API spend is bounded no matter how many
 * users or requests arrive. Limits are configurable in application.properties.
 */
@Service
public class UsageService {

    @Value("${usage.user.monthly-limit:10}")
    private int userMonthlyLimit;

    // Tope diario de "uso justo" para usuarios PREMIUM. Premium no tiene límite
    // mensual (es "ilimitado"), pero un tope diario alto evita que una sola cuenta
    // abusiva dispare el coste. Tan alto que ningún usuario real lo nota.
    @Value("${usage.premium.daily-limit:100}")
    private int premiumDailyLimit;

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

        // ——— Admins: fully exempt from personal quotas (so the owner can test/demo
        // freely), but the global kill-switch still applies as a hard cost fuse. ———
        if (isAdmin) {
            consumeGlobalDaily(today);
            return;
        }

        // ——— PREMIUM: no monthly limit ("unlimited") and EXEMPT from the global fuse
        // (they paid for unlimited — the shared cap must never tell a paying user "come
        // back tomorrow", and their usage must not push free users toward that cap).
        // The only guard is a high per-user DAILY safety cap against a single abusive
        // account. Premium usage does NOT touch the global counter. ———
        if (user.isPremiumActive()) {
            if (user.getUsageDayStart() == null || !user.getUsageDayStart().equals(today)) {
                user.setUsageDayStart(today);
                user.setDailyTextCount(0);
            }
            if (user.getDailyTextCount() >= premiumDailyLimit) {
                throw new UsageLimitException(
                        "You've reached today's fair-use limit of " + premiumDailyLimit
                                + " text creations. It resets tomorrow.");
            }
            user.setDailyTextCount(user.getDailyTextCount() + 1);
            userRepository.save(user);
            return;
        }

        // ——— FREE plan: per-user monthly quota AND the global daily kill-switch. ———
        LocalDate monthStart = today.withDayOfMonth(1);
        if (user.getUsagePeriodStart() == null || !user.getUsagePeriodStart().equals(monthStart)) {
            user.setUsagePeriodStart(monthStart);
            user.setMonthlyTextCount(0);
        }
        if (user.getMonthlyTextCount() >= userMonthlyLimit) {
            throw new UsageLimitException(
                    "You've reached your free monthly limit of " + userMonthlyLimit
                            + " text creations. Upgrade to Premium for unlimited, or wait until next month.");
        }
        AppUsage usage = appUsageRepository.findByDay(today).orElseGet(() -> new AppUsage(today, 0));
        if (usage.getCount() >= globalDailyLimit) {
            throw new UsageLimitException(
                    "Text creation is temporarily unavailable for today. Please try again tomorrow.");
        }
        user.setMonthlyTextCount(user.getMonthlyTextCount() + 1);
        userRepository.save(user);
        usage.setCount(usage.getCount() + 1);
        appUsageRepository.save(usage);
    }

    /** Checks and consumes one unit from the global daily kill-switch. */
    private void consumeGlobalDaily(LocalDate today) {
        AppUsage usage = appUsageRepository.findByDay(today).orElseGet(() -> new AppUsage(today, 0));
        if (usage.getCount() >= globalDailyLimit) {
            throw new UsageLimitException(
                    "Text creation is temporarily unavailable for today. Please try again tomorrow.");
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
