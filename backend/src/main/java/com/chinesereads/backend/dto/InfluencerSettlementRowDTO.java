package com.chinesereads.backend.dto;

import java.util.List;

/**
 * One influencer's settlement for the requested period: customer counters, charge
 * counters (which drive the payout: charges × per-charge commission) and the
 * customer detail list. {@code active} counts customers whose paid coverage
 * extends beyond the period's end, whatever their status label.
 */
public record InfluencerSettlementRowDTO(
        String code,
        Integer newCustomers,
        Integer renewals,
        Integer churned,
        Integer active,
        Integer monthlyCharges,
        Integer yearlyCharges,
        Long payoutCents,
        List<InfluencerSettlementCustomerDTO> customers) {
}
