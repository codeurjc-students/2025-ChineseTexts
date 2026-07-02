package com.chinesereads.backend.Service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TtsService {

    @Value("${tts.service.url:http://localhost:5002}")
    private String ttsServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Sends the Chinese text to the Flask TTS microservice and returns the
     * synthesized speech as MP3 bytes.
     */
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
