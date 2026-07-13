package com.chinesereads.backend.Controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.AudioUsageService;
import com.chinesereads.backend.Service.TtsRateLimiterService;
import com.chinesereads.backend.Service.TtsService;
import com.chinesereads.backend.Service.UsageLimitException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Text-to-Speech endpoint. Audio calls the paid Google Cloud TTS API, so it is
 * registered-only (see SecurityConfig: {@code /api/tts/**} requires USER/ADMIN):
 * anonymous visitors must sign up to listen.
 *
 * Every miss calls the paid API, so it is protected on several fronts: a hard length
 * cap (blocks a single huge request), a per-IP rate limit (blocks hammering), a
 * text-keyed cache in {@link TtsService} (avoids re-paying for identical input), and
 * a per-user monthly quota split by play type ({@link AudioUsageService}) — premium
 * and admins exempt.
 */
@CrossOrigin
@RestController
@RequestMapping("/api/tts")
public class TtsController {

    private final TtsService ttsService;

    private final TtsRateLimiterService rateLimiter;

    private final AudioUsageService audioUsageService;

    private final UserRepository userRepository;

    /** Maximum characters accepted; the Flask TTS service itself caps around 1600. */
    @Value("${tts.max-chars:1600}")
    private int maxChars;

    public TtsController(TtsService ttsService, TtsRateLimiterService rateLimiter, AudioUsageService audioUsageService, UserRepository userRepository) {
        this.ttsService = ttsService;
        this.rateLimiter = rateLimiter;
        this.audioUsageService = audioUsageService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<byte[]> synthesize(HttpServletRequest request,
                                             @RequestBody(required = false) Map<String, String> body) {
        // Registered-only: resolve the authenticated user (SecurityConfig already
        // blocks anonymous requests, but guard defensively).
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

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

        // Per-user monthly audio quota: single-word plays and sentence/full-text plays
        // are metered separately (the frontend sends "type": "word" | "phrase").
        boolean isWord = body != null && "word".equalsIgnoreCase(body.get("type"));
        try {
            audioUsageService.reserveAudio(user, isWord);
        } catch (UsageLimitException e) {
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
