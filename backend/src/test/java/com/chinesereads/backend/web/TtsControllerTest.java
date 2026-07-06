package com.chinesereads.backend.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.chinesereads.backend.Service.TtsService;

/**
 * Verifies the cost-protection guards on the public /api/tts endpoint: the length
 * cap (413), the per-IP rate limit (429) and the happy path (200). TtsService is
 * mocked so no real (paid) synthesis happens. Security filters are bypassed via a
 * standalone-style MockMvc so this focuses purely on the controller logic.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = { "tts.max-chars=5", "tts.rate-limit.per-minute=2" })
public class TtsControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private TtsService ttsService;

    private MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    private static final String JSON = "application/json";

    @Test
    @DisplayName("Blank text is rejected with 400")
    public void testBlankText() throws Exception {
        mvc().perform(post("/api/tts").contentType(JSON).content("{\"text\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Text longer than the cap is rejected with 413")
    public void testTooLong() throws Exception {
        // maxChars=5; this text has 6 characters.
        mvc().perform(post("/api/tts").header("X-Forwarded-For", "9.9.9.1")
                        .contentType(JSON).content("{\"text\":\"你好你好你好\"}"))
                .andExpect(status().is(413));
    }

    @Test
    @DisplayName("Valid text within the cap returns 200 audio")
    public void testValid() throws Exception {
        when(ttsService.synthesize(anyString())).thenReturn(new byte[] { 1, 2, 3 });

        mvc().perform(post("/api/tts").header("X-Forwarded-For", "9.9.9.2")
                        .contentType(JSON).content("{\"text\":\"你好\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Requests beyond the per-minute limit are rejected with 429")
    public void testRateLimit() throws Exception {
        when(ttsService.synthesize(anyString())).thenReturn(new byte[] { 1 });

        // per-minute=2 for a single IP: first two pass, the third is throttled.
        mvc().perform(post("/api/tts").header("X-Forwarded-For", "9.9.9.3")
                        .contentType(JSON).content("{\"text\":\"你\"}")).andExpect(status().isOk());
        mvc().perform(post("/api/tts").header("X-Forwarded-For", "9.9.9.3")
                        .contentType(JSON).content("{\"text\":\"好\"}")).andExpect(status().isOk());
        mvc().perform(post("/api/tts").header("X-Forwarded-For", "9.9.9.3")
                        .contentType(JSON).content("{\"text\":\"吗\"}")).andExpect(status().isTooManyRequests());
    }
}
