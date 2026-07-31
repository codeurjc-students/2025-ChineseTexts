package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.chinesereads.backend.Service.PasswordResetRateLimiterService;

/**
 * Pins the per-IP hourly window of the forgot-password limiter: requests up to the
 * limit pass, the next one is rejected, and keys (IPs) are counted independently.
 */
public class PasswordResetRateLimiterServiceTest {

    // Test unitario 1: permite hasta el límite y rechaza la siguiente
    @Test
    @DisplayName("Allows up to the hourly limit and rejects the next request")
    public void testLimitEnforced() {
        PasswordResetRateLimiterService limiter = new PasswordResetRateLimiterService(3);
        assertTrue(limiter.tryAcquire("1.2.3.4"));
        assertTrue(limiter.tryAcquire("1.2.3.4"));
        assertTrue(limiter.tryAcquire("1.2.3.4"));
        assertFalse(limiter.tryAcquire("1.2.3.4"));
    }

    // Test unitario 2: cada IP cuenta por separado
    @Test
    @DisplayName("Each client key has its own independent counter")
    public void testKeysAreIndependent() {
        PasswordResetRateLimiterService limiter = new PasswordResetRateLimiterService(1);
        assertTrue(limiter.tryAcquire("1.1.1.1"));
        assertFalse(limiter.tryAcquire("1.1.1.1"));
        assertTrue(limiter.tryAcquire("2.2.2.2")); // otra IP no se ve afectada
    }
}
