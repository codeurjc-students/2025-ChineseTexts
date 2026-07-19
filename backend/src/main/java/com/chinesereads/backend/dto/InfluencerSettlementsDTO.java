package com.chinesereads.backend.dto;

import java.time.LocalDate;
import java.util.List;

/** The full settlements response: the echoed period, the configured per-charge
 * commissions (for display) and one row per influencer with activity in range. */
public record InfluencerSettlementsDTO(
        LocalDate from,
        LocalDate to,
        Long payoutMonthlyCents,
        Long payoutYearlyCents,
        List<InfluencerSettlementRowDTO> rows) {
}
