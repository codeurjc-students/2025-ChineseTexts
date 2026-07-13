package com.chinesereads.backend.Service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;

/**
 * Enforces the per-user monthly quota for the AI contextual word chat. Each message
 * calls the paid DeepSeek API, so the chat is registered-only and metered: free users
 * get a small monthly taste, while premium subscribers and admins are unlimited.
 * Limit configurable via {@code usage.chat.monthly-limit}.
 */
@Service
public class ChatUsageService {

    @Value("${usage.chat.monthly-limit:10}")
    private int chatMonthlyLimit;

    private final UserRepository userRepository;

    public ChatUsageService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** True when the user has no monthly chat cap (active premium or admin). */
    public boolean isUnlimited(User user) {
        boolean isAdmin = user.getRoles() != null && user.getRoles().contains("ADMIN");
        return isAdmin || user.isPremiumActive();
    }

    public int getMonthlyLimit() {
        return chatMonthlyLimit;
    }

    /**
     * Verifies the monthly chat quota and, if allowed, consumes one message. Call before
     * each AI call. Admins and active premium users are exempt.
     *
     * @throws UsageLimitException if the free monthly quota is reached.
     */
    @Transactional
    public void reserveChat(User user) {
        if (isUnlimited(user)) {
            return;
        }
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        if (user.getChatUsagePeriodStart() == null
                || !user.getChatUsagePeriodStart().equals(monthStart)) {
            user.setChatUsagePeriodStart(monthStart);
            user.setMonthlyChatCount(0);
        }
        if (user.getMonthlyChatCount() >= chatMonthlyLimit) {
            throw new UsageLimitException(
                    "You've reached your free monthly limit of " + chatMonthlyLimit
                            + " AI chat messages. Upgrade to Premium for unlimited AI tutoring.");
        }
        user.setMonthlyChatCount(user.getMonthlyChatCount() + 1);
        userRepository.save(user);
    }
}
