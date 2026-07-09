package com.chinesereads.backend.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.AudioUsageService;
import com.chinesereads.backend.Service.TtsService;
import com.chinesereads.backend.Service.UsageLimitException;

/**
 * Verifies the guards on the registered-only /api/tts endpoint: unauthenticated (401),
 * the length cap (413), the per-IP rate limit (429), the per-user audio quota (429) and
 * the happy path (200). TtsService, UserRepository and AudioUsageService are mocked so
 * no real (paid) synthesis or DB access happens. Security filters are bypassed via a
 * standalone-style MockMvc, so the authenticated user is supplied via the request
 * principal to focus purely on the controller logic.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = { "tts.max-chars=5", "tts.rate-limit.per-minute=2" })
public class TtsControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private TtsService ttsService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AudioUsageService audioUsageService;

    private final Principal principal = () -> "u@u.com";

    @BeforeEach
    public void setUp() {
        when(userRepository.findByEmail("u@u.com"))
                .thenReturn(Optional.of(new User("u@u.com", "U", "pass", "en", "USER")));
    }

    private MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    private static final String JSON = "application/json";

    @Test
    @DisplayName("Anonymous request is rejected with 401")
    public void testAnonymous() throws Exception {
        mvc().perform(post("/api/tts").contentType(JSON).content("{\"text\":\"你好\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Blank text is rejected with 400")
    public void testBlankText() throws Exception {
        mvc().perform(post("/api/tts").principal(principal).contentType(JSON).content("{\"text\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Text longer than the cap is rejected with 413")
    public void testTooLong() throws Exception {
        // maxChars=5; this text has 6 characters.
        mvc().perform(post("/api/tts").principal(principal).header("X-Forwarded-For", "9.9.9.1")
                        .contentType(JSON).content("{\"text\":\"你好你好你好\"}"))
                .andExpect(status().is(413));
    }

    @Test
    @DisplayName("Valid text within the cap returns 200 audio")
    public void testValid() throws Exception {
        when(ttsService.synthesize(anyString())).thenReturn(new byte[] { 1, 2, 3 });

        mvc().perform(post("/api/tts").principal(principal).header("X-Forwarded-For", "9.9.9.2")
                        .contentType(JSON).content("{\"text\":\"你好\",\"type\":\"word\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Exceeding the per-user audio quota is rejected with 429")
    public void testAudioQuotaExceeded() throws Exception {
        doThrow(new UsageLimitException("limit")).when(audioUsageService).reserveAudio(any(), anyBoolean());

        mvc().perform(post("/api/tts").principal(principal).header("X-Forwarded-For", "9.9.9.4")
                        .contentType(JSON).content("{\"text\":\"你好\",\"type\":\"phrase\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Requests beyond the per-minute limit are rejected with 429")
    public void testRateLimit() throws Exception {
        when(ttsService.synthesize(anyString())).thenReturn(new byte[] { 1 });

        // per-minute=2 for a single IP: first two pass, the third is throttled.
        mvc().perform(post("/api/tts").principal(principal).header("X-Forwarded-For", "9.9.9.3")
                        .contentType(JSON).content("{\"text\":\"你\"}")).andExpect(status().isOk());
        mvc().perform(post("/api/tts").principal(principal).header("X-Forwarded-For", "9.9.9.3")
                        .contentType(JSON).content("{\"text\":\"好\"}")).andExpect(status().isOk());
        mvc().perform(post("/api/tts").principal(principal).header("X-Forwarded-For", "9.9.9.3")
                        .contentType(JSON).content("{\"text\":\"吗\"}")).andExpect(status().isTooManyRequests());
    }
}
