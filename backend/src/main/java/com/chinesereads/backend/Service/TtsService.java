package com.chinesereads.backend.Service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.chinesereads.backend.Config.CacheConfig;

@Service
public class TtsService {

    @Value("${tts.service.url:http://localhost:5002}")
    private String ttsServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Sends the Chinese text to the Flask TTS microservice and returns the
     * synthesized speech as MP3 bytes.
     *
     * Results are cached by the exact text: identical input (a replayed word,
     * sentence or flashcard) is served from memory without re-calling the paid
     * Google Cloud TTS API. Empty results are never cached so a transient failure
     * doesn't get pinned.
     */
    @Cacheable(cacheNames = CacheConfig.TTS_AUDIO_CACHE, key = "#text",
               unless = "#result == null || #result.length == 0")
    public byte[] synthesize(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.valueOf("audio/mpeg"), MediaType.ALL));

        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("text", text), headers);

        return restTemplate.exchange(
                ttsServiceUrl + "/synthesize",
                HttpMethod.POST,
                request,
                byte[].class
        ).getBody();
    }
}
