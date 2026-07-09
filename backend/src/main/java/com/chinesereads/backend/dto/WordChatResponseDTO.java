package com.chinesereads.backend.dto;

/**
 * The AI's reply plus the user's remaining chat quota, so the UI can show a dwindling
 * "X messages left" counter (or hide it for unlimited premium/admin users).
 *
 * @param reply      the assistant's message
 * @param unlimited  true for premium/admin (no monthly cap)
 * @param remaining  messages left this month (free plan only; 0 when unlimited)
 */
public record WordChatResponseDTO(String reply, boolean unlimited, int remaining) {
}
