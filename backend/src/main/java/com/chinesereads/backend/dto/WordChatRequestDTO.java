package com.chinesereads.backend.dto;

import java.util.List;

/**
 * A turn in the AI contextual word chat. The client sends the word and its context
 * (sentence, full text, translation, optional HSK level), the active UI language, and
 * the running message history (ending with the user's latest message). The chat is
 * stateless on the server — the whole history is re-sent each turn.
 */
public record WordChatRequestDTO(
        String word,
        String sentence,
        String text,
        String translation,
        String level,
        String language,
        List<ChatMessageDTO> history) {

    /** One chat message; role is "user" or "assistant". */
    public record ChatMessageDTO(String role, String content) {
    }
}
