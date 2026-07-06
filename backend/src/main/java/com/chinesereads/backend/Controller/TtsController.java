package com.chinesereads.backend.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chinesereads.backend.Service.TtsRateLimiterService;
import com.chinesereads.backend.Service.TtsService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Public Text-to-Speech endpoint. Reading a text is available to anonymous
 * users, so this endpoint is intentionally public (no matcher in SecurityConfig,
 * covered by the trailing anyRequest().permitAll()).
 *
 * Because it is public and every miss calls the paid Google Cloud TTS API, it is
 * protected on three fronts: a hard length cap (blocks a single huge request), a
 * per-IP rate limit (blocks hammering), and a text-keyed cache in {@link TtsService}
 * (avoids re-paying for identical input).
 */
@CrossOrigin
@RestController
@RequestMapping("/api/tts")
public class TtsController {

    @Autowired
    private TtsService ttsService;

    @Autowired
    private TtsRateLimiterService rateLimiter;

    /** Maximum characters accepted; the Flask TTS service itself caps around 1600. */
    @Value("${tts.max-chars:1600}")
    private int maxChars;

    @PostMapping
    public ResponseEntity<byte[]> synthesize(HttpServletRequest request,
                                             @RequestBody(required = false) Map<String, String> body) {
        String text = body != null ? body.get("text") : null;
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        text = text.trim();
        if (text.length() > maxChars) {
            return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).build();
        }

        if (!rateLimiter.tryAcquire(clientKey(request))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        try {
            byte[] audio = ttsService.synthesize(text);
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

    /**
     * Best-effort client identifier for rate limiting. The app runs behind the
     * Caddy reverse proxy, which forwards the real client IP in X-Forwarded-For;
     * fall back to the direct remote address when the header is absent.
     */
    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For may be a comma-separated list; the first entry is the client.
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String remote = request.getRemoteAddr();
        return remote != null ? remote : "unknown";
    }
}
