package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.chinesereads.backend.Service.LoginRateLimiterService;

/**
 * Pins the brute-force budget of the login: failures count per email and per IP,
 * whichever cap is hit first blocks, a success forgives the account (not the IP),
 * and the email key is case-insensitive so "Admin@x.com" cannot dodge the cap.
 */
public class LoginRateLimiterServiceTest {

    private static final String IP = "203.0.113.7";
    private static final String EMAIL = "maria@test.com";

    @Test
    @DisplayName("Nothing is blocked before any failure, and failures below the cap are tolerated")
    public void underTheCapIsAllowed() {
        LoginRateLimiterService limiter = new LoginRateLimiterService(30, 3, 15);
        assertFalse(limiter.isBlocked(IP, EMAIL));
        limiter.recordFailure(IP, EMAIL);
        limiter.recordFailure(IP, EMAIL);
        assertFalse(limiter.isBlocked(IP, EMAIL));
    }

    @Test
    @DisplayName("Reaching the per-email cap blocks that account from any IP")
    public void perEmailCapBlocksAcrossIps() {
        LoginRateLimiterService limiter = new LoginRateLimiterService(30, 3, 15);
        limiter.recordFailure("1.1.1.1", EMAIL);
        limiter.recordFailure("2.2.2.2", EMAIL);
        limiter.recordFailure("3.3.3.3", EMAIL);
        assertTrue(limiter.isBlocked("4.4.4.4", EMAIL), "a distributed attack on one account is stopped");
        assertFalse(limiter.isBlocked("4.4.4.4", "other@test.com"), "other accounts are untouched");
    }

    @Test
    @DisplayName("Reaching the per-IP cap blocks that client for any email")
    public void perIpCapBlocksAcrossEmails() {
        LoginRateLimiterService limiter = new LoginRateLimiterService(2, 30, 15);
        limiter.recordFailure(IP, "a@test.com");
        limiter.recordFailure(IP, "b@test.com");
        assertTrue(limiter.isBlocked(IP, "c@test.com"), "one client guessing many accounts is stopped");
        assertFalse(limiter.isBlocked("198.51.100.9", "c@test.com"), "other clients are untouched");
    }

    @Test
    @DisplayName("A successful login clears the account's failures but not the IP budget")
    public void successForgivesTheAccountOnly() {
        LoginRateLimiterService limiter = new LoginRateLimiterService(3, 2, 15);
        limiter.recordFailure(IP, EMAIL);
        limiter.recordFailure(IP, EMAIL);
        assertTrue(limiter.isBlocked(IP, EMAIL));

        limiter.recordSuccess(EMAIL);
        assertFalse(limiter.isBlocked(IP, EMAIL), "the account's counter is gone");

        // The two earlier failures still count against the IP: one more hits its cap of 3.
        limiter.recordFailure(IP, "other@test.com");
        assertTrue(limiter.isBlocked(IP, "whoever@test.com"));
    }

    @Test
    @DisplayName("The email key ignores case and surrounding spaces")
    public void emailKeyIsNormalised() {
        LoginRateLimiterService limiter = new LoginRateLimiterService(30, 2, 15);
        limiter.recordFailure("1.1.1.1", "Admin@Test.com");
        limiter.recordFailure("2.2.2.2", " admin@test.com ");
        assertTrue(limiter.isBlocked("3.3.3.3", "ADMIN@TEST.COM"));
    }
}
