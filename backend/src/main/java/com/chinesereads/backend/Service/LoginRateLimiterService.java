package com.chinesereads.backend.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Brute-force protection for POST /api/auth/login. Only FAILED attempts count, and
 * they count twice: per client IP (one attacker trying many accounts) and per email
 * (many IPs trying one account, e.g. the admin's). Once either counter reaches its
 * cap the login is refused with 429 until the fixed window expires, even with the
 * right password. A successful login clears the email counter so a user who
 * mistyped a couple of times is not penalised afterwards; the IP counter is left
 * alone on purpose so owning one valid account does not reset an attacker's budget.
 *
 * Same Caffeine pattern as {@link PasswordResetRateLimiterService}: entries expire
 * on their own and memory stays bounded. Limits are generous by design — a whole
 * classroom behind one NAT must never be locked out by a few typos.
 */
@Service
public class LoginRateLimiterService {

    private final int maxPerIp;
    private final int maxPerEmail;
    private final Cache<String, AtomicInteger> failures;

    public LoginRateLimiterService(
            @Value("${login.rate-limit.per-ip:30}") int maxPerIp,
            @Value("${login.rate-limit.per-email:10}") int maxPerEmail,
            @Value("${login.rate-limit.window-minutes:15}") int windowMinutes) {
        this.maxPerIp = maxPerIp;
        this.maxPerEmail = maxPerEmail;
        this.failures = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(windowMinutes))
                .maximumSize(20_000)
                .build();
    }

    /** True when this IP or this email has already used up its failed attempts. */
    public boolean isBlocked(String ip, String email) {
        return count(ipKey(ip)) >= maxPerIp || count(emailKey(email)) >= maxPerEmail;
    }

    /** Records one failed attempt against both keys. */
    public void recordFailure(String ip, String email) {
        failures.get(ipKey(ip), k -> new AtomicInteger(0)).incrementAndGet();
        failures.get(emailKey(email), k -> new AtomicInteger(0)).incrementAndGet();
    }

    /** A correct login forgives that account's previous typos (the IP budget stays). */
    public void recordSuccess(String email) {
        failures.invalidate(emailKey(email));
    }

    private int count(String key) {
        AtomicInteger counter = failures.getIfPresent(key);
        return counter == null ? 0 : counter.get();
    }

    private static String ipKey(String ip) {
        return "ip:" + (ip == null ? "unknown" : ip);
    }

    private static String emailKey(String email) {
        return "email:" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }
}
