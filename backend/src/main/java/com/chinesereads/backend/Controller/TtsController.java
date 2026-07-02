package com.chinesereads.backend.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chinesereads.backend.Service.TtsService;

/**
 * Public Text-to-Speech endpoint. Reading a text is available to anonymous
 * users, so this endpoint is intentionally public (no matcher in SecurityConfig,
 * covered by the trailing anyRequest().permitAll()).
 */
@CrossOrigin
@RestController
@RequestMapping("/api/tts")
public class TtsController {

    @Autowired
    private TtsService ttsService;

    @PostMapping
    public ResponseEntity<byte[]> synthesize(@RequestBody(required = false) Map<String, String> body) {
        String text = body != null ? body.get("text") : null;
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            byte[] audio = ttsService.synthesize(text.trim());
            if (audio == null || audio.length == 0) {
                return ResponseEntity.status(502).build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .body(audio);
        } catch (Exception e) {
            return ResponseEntity.status(503).build();
        }
    }
}
