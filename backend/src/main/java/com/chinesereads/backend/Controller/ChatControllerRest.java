package com.chinesereads.backend.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.AiService;
import com.chinesereads.backend.Service.ChatUsageService;
import com.chinesereads.backend.Service.UsageLimitException;
import com.chinesereads.backend.dto.WordChatRequestDTO;
import com.chinesereads.backend.dto.WordChatResponseDTO;

import jakarta.servlet.http.HttpServletRequest;

/**
 * AI contextual word chat — a registered-only, guardrailed tutor for one word in its
 * reading context. Each message calls the paid AI service, so it is metered per user
 * ({@link ChatUsageService}): free users get a small monthly quota, premium/admins are
 * unlimited. The conversation is stateless here — the client re-sends the history each
 * turn (ephemeral).
 */
@CrossOrigin
@RestController
@RequestMapping("/api/chat")
public class ChatControllerRest {

    @Autowired
    private AiService aiService;

    @Autowired
    private ChatUsageService chatUsageService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/word")
    public ResponseEntity<?> chatWord(@RequestBody(required = false) WordChatRequestDTO body,
                                      HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (body == null || body.word() == null || body.word().isBlank()
                || body.history() == null || body.history().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Reserve one message BEFORE the paid AI call, so a user at their limit can't
        // trigger it (cost cap). Premium/admins are exempt.
        try {
            chatUsageService.reserveChat(user);
        } catch (UsageLimitException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        List<Map<String, String>> history = body.history().stream()
                .map(m -> Map.of(
                        "role", m.role() == null ? "" : m.role(),
                        "content", m.content() == null ? "" : m.content()))
                .toList();
        String language = (body.language() != null && !body.language().isBlank())
                ? body.language() : user.getLanguage();

        String reply;
        try {
            reply = aiService.chatAboutWord(body.word(), body.sentence(), body.text(),
                    body.translation(), body.level(), language, history);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
        if (reply == null || reply.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }

        boolean unlimited = chatUsageService.isUnlimited(user);
        int remaining = unlimited ? 0
                : Math.max(0, chatUsageService.getMonthlyLimit() - user.getMonthlyChatCount());
        return ResponseEntity.ok(new WordChatResponseDTO(reply, unlimited, remaining));
    }
}
