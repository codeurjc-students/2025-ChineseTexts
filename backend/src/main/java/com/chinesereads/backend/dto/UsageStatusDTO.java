package com.chinesereads.backend.dto;

/**
 * The authenticated user's current text-generation usage, for the meter shown on the
 * "My Tools" page. Premium and admins are unlimited (used/limit are 0 and ignored);
 * free users get their monthly count and cap so the UI can show "X of N used".
 *
 * @param plan       "free", "premium" or "admin"
 * @param unlimited  true for premium/admin (no monthly cap)
 * @param used       generations used this month (free plan only)
 * @param limit      monthly generation cap (free plan only)
 */
public record UsageStatusDTO(String plan, boolean unlimited, int used, int limit) {
}
