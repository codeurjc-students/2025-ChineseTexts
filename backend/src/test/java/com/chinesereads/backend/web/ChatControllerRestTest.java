package com.chinesereads.backend.web;

import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.AiService;
import com.chinesereads.backend.Service.ChatUsageService;
import com.chinesereads.backend.Service.UsageLimitException;

/**
 * Verifies the AI word-chat endpoint guards: unauthenticated (401), bad body (400),
 * the per-user quota (429) and the happy path (200). AiService, ChatUsageService and
 * UserRepository are mocked so no real (paid) AI call or DB access happens; the
 * authenticated user is supplied via the request principal.
 */
@SpringBootTest
@ActiveProfiles("test")
public class ChatControllerRestTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AiService aiService;

    @MockitoBean
    private ChatUsageService chatUsageService;

    @MockitoBean
    private UserRepository userRepository;

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
    private static final String VALID_BODY =
            "{\"word\":\"你好\",\"history\":[{\"role\":\"user\",\"content\":\"explain\"}],\"language\":\"en\"}";

    @Test
    @DisplayName("Anonymous request is rejected with 401")
    public void testAnonymous() throws Exception {
        mvc().perform(post("/api/chat/word").contentType(JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Empty history is rejected with 400")
    public void testBadBody() throws Exception {
        mvc().perform(post("/api/chat/word").principal(principal).contentType(JSON)
                        .content("{\"word\":\"你好\",\"history\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Exceeding the monthly chat quota is rejected with 429")
    public void testQuotaExceeded() throws Exception {
        doThrow(new UsageLimitException("limit")).when(chatUsageService).reserveChat(any());

        mvc().perform(post("/api/chat/word").principal(principal).contentType(JSON).content(VALID_BODY))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("A valid request returns 200 with the AI reply")
    public void testValid() throws Exception {
        when(aiService.chatAboutWord(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("你好 (nǐ hǎo) means hello.");

        mvc().perform(post("/api/chat/word").principal(principal).contentType(JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }
}
