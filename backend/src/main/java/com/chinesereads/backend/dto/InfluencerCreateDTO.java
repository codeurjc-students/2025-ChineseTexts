package com.chinesereads.backend.dto;

/**
 * Request body for creating an influencer discount code. Every field is a nullable
 * wrapper on purpose: Jackson 3 rejects bodies that omit a primitive field, so
 * validation (required-ness, ranges) happens in the service instead.
 */
public record InfluencerCreateDTO(
        String code,
        Long percentOff,
        // "once" (first payment), "repeating" (durationInMonths months) or "forever".
        String duration,
        Long durationInMonths,
        // Anti-"discount farming": restrict the code to customers with no previous
        // payment (Stripe first_time_transaction). Null defaults to TRUE — the safe
        // choice; sending false explicitly enables win-back campaigns.
        Boolean firstTimeOnly) {
}
