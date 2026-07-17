package com.chinesereads.backend.dto;

/**
 * One row of the influencer tracking panel: a discount code with its Stripe terms plus
 * the locally-measured funnel (signups from ?ref links, checkout conversions, and how
 * many of those conversions still hold an active premium).
 *
 * <p>Stripe-side fields ({@code id}, {@code active}, {@code percentOff}, ...) are null
 * for codes that only exist as ?ref sources (e.g. a link campaign with no discount).
 */
public record InfluencerCodeDTO(
        String id,
        String code,
        Boolean active,
        Long percentOff,
        String duration,
        Long durationInMonths,
        Long timesRedeemed,
        long signups,
        long conversions,
        long activePremium) {
}
