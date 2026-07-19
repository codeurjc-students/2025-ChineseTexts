package com.chinesereads.backend.dto;

import java.time.LocalDate;

/**
 * One customer inside an influencer settlement row. {@code status} within the
 * requested period: "new" (first charge in range), "renewal" (charged in range,
 * was already a customer), "churned" (paid coverage ran out inside the range
 * without a new charge) or "active" (covered through the range with no charge in
 * it — e.g. a yearly customer mid-period). {@code payoutCents} is the commission
 * this customer generated within the range.
 */
public record InfluencerSettlementCustomerDTO(
        Long userId,
        String username,
        String plan,
        String status,
        Integer charges,
        Long payoutCents,
        LocalDate firstPaidOn,
        LocalDate lastPaidOn,
        LocalDate coveredUntil) {
}
