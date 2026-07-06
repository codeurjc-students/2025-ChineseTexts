package com.chinesereads.backend.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Per-IP fixed-window rate limiter for the public TTS endpoint.
 *
 * The endpoint is unauthenticated and each miss calls the paid Google Cloud TTS
 * API, so a single client hammering it is a real cost/abuse risk. This caps how
 * many requests one IP may make per minute. The window state lives in a Caffeine
 * cache whose entries expire one minute after creation, so counters self-clean
 * and memory is bounded regardless of how many distinct IPs are seen.
 */
@Service
public class TtsRateLimiterService {

    private final int maxPerMinute;
    private final Cache<String, AtomicInteger> counters;

    public TtsRateLimiterService(@Value("${tts.rate-limit.per-minute:60}") int maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
        this.counters = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .maximumSize(10_000)
                .build();
    }

    /**
     * Registers one request from the given client key and reports whether it is
     * allowed. Returns false once the per-minute limit for that key is exceeded.
     */
    public boolean tryAcquire(String clientKey) {
        AtomicInteger counter = counters.get(clientKey, k -> new AtomicInteger(0));
        return counter.incrementAndGet() <= maxPerMinute;
    }
}
