package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.chinesereads.backend.Service.TtsRateLimiterService;

/**
 * Unit tests for the per-IP TTS rate limiter. Verifies the per-minute cap and
 * that different clients are tracked independently.
 */
public class TtsRateLimiterServiceTest {

    @Test
    @DisplayName("Allows up to the limit, then rejects further requests for the same client")
    public void testCapPerClient() {
        TtsRateLimiterService limiter = new TtsRateLimiterService(3);

        assertTrue(limiter.tryAcquire("1.2.3.4"));
        assertTrue(limiter.tryAcquire("1.2.3.4"));
        assertTrue(limiter.tryAcquire("1.2.3.4"));
        assertFalse(limiter.tryAcquire("1.2.3.4"));
        assertFalse(limiter.tryAcquire("1.2.3.4"));
    }

    @Test
    @DisplayName("Tracks each client independently")
    public void testClientsAreIndependent() {
        TtsRateLimiterService limiter = new TtsRateLimiterService(1);

        assertTrue(limiter.tryAcquire("10.0.0.1"));
        assertFalse(limiter.tryAcquire("10.0.0.1"));   // first client exhausted
        assertTrue(limiter.tryAcquire("10.0.0.2"));    // second client unaffected
    }
}
