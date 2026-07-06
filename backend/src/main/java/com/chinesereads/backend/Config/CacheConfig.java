package com.chinesereads.backend.Config;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Enables application caching and configures the Caffeine-backed cache used to
 * store synthesized Text-to-Speech audio.
 *
 * The TTS endpoint is public and every miss calls the paid Google Cloud TTS API.
 * The same words, sentences and flashcards are replayed constantly, so caching
 * the MP3 bytes by their exact text avoids re-paying for identical input — the
 * single biggest ongoing cost saving for this endpoint. The cache is bounded in
 * both entries and time so memory stays under control.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Cache name for synthesized TTS audio (byte[] MP3 keyed by the exact text). */
    public static final String TTS_AUDIO_CACHE = "ttsAudio";

    @Value("${tts.cache.max-entries:1000}")
    private long maxEntries;

    @Value("${tts.cache.ttl-days:30}")
    private long ttlDays;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(List.of(TTS_AUDIO_CACHE));
        manager.setAllowNullValues(false);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(maxEntries)
                .expireAfterWrite(Duration.ofDays(ttlDays)));
        return manager;
    }
}
