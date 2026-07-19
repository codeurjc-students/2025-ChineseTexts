package com.chinesereads.backend.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.chinesereads.backend.Model.InfluencerPayment;
import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.InfluencerPaymentRepository;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.StripeService.PromoCode;
import com.chinesereads.backend.dto.InfluencerCodeDTO;
import com.chinesereads.backend.dto.InfluencerCreateDTO;
import com.chinesereads.backend.dto.InfluencerSettlementCustomerDTO;
import com.chinesereads.backend.dto.InfluencerSettlementRowDTO;
import com.chinesereads.backend.dto.InfluencerSettlementsDTO;
import com.stripe.exception.StripeException;

/**
 * Influencer campaign tracking. Merges two attribution sources into one table:
 *
 * <ul>
 *   <li><b>Stripe promotion codes</b> — the discount codes influencers share; Stripe
 *       counts every redemption ({@code times_redeemed}) and the webhook additionally
 *       records which user redeemed which code (conversions / active premium).</li>
 *   <li><b>?ref signup links</b> — {@code chinesereads.com/?ref=CODE} visits store the
 *       code client-side and attach it at signup, measuring registrations per code even
 *       when the visitor never pays.</li>
 * </ul>
 *
 * The merge is pure and unit-tested; Stripe I/O stays behind {@link StripeService}.
 */
@Service
public class InfluencerService {

    private static final Logger log = LoggerFactory.getLogger(InfluencerService.class);

    /** Codes are shared verbally and in video captions: short, unambiguous charset. */
    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Za-z0-9_-]{3,40}");

    private final StripeService stripeService;
    private final UserRepository userRepository;
    private final InfluencerPaymentRepository paymentRepository;
    private final long payoutMonthlyCents;
    private final long payoutYearlyCents;

    public InfluencerService(StripeService stripeService, UserRepository userRepository,
            InfluencerPaymentRepository paymentRepository,
            @Value("${influencer.payout.monthly-cents:200}") long payoutMonthlyCents,
            @Value("${influencer.payout.yearly-cents:1800}") long payoutYearlyCents) {
        this.stripeService = stripeService;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.payoutMonthlyCents = payoutMonthlyCents;
        this.payoutYearlyCents = payoutYearlyCents;
    }

    /** The full tracking table: every Stripe code plus every ref-only code seen at signup. */
    public List<InfluencerCodeDTO> getStats() throws StripeException {
        return merge(stripeService.listPromotionCodes(),
                userRepository.findByReferralSourceIsNotNull(),
                userRepository.findByStripePromotionCodeIdIsNotNull());
    }

    /**
     * Validates and creates a new influencer code in Stripe. The code is normalized to
     * upper case (checkout entry is case-insensitive on Stripe's side, and so is our
     * ?ref matching). Duplicate ACTIVE codes are rejected up front with a clear error —
     * Stripe allows reusing the string of a deactivated code, and so do we.
     */
    public InfluencerCodeDTO create(InfluencerCreateDTO request) throws StripeException {
        if (request.code() == null || !CODE_PATTERN.matcher(request.code().trim()).matches()) {
            throw new IllegalArgumentException("INVALID_CODE");
        }
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (request.percentOff() == null || request.percentOff() < 1 || request.percentOff() > 100) {
            throw new IllegalArgumentException("INVALID_PERCENT");
        }
        String duration = request.duration() == null ? "once" : request.duration();
        if (!List.of("once", "repeating", "forever").contains(duration)) {
            throw new IllegalArgumentException("INVALID_DURATION");
        }
        if ("repeating".equals(duration)
                && (request.durationInMonths() == null || request.durationInMonths() < 1
                        || request.durationInMonths() > 36)) {
            throw new IllegalArgumentException("INVALID_MONTHS");
        }
        boolean activeDuplicate = stripeService.listPromotionCodes().stream()
                .anyMatch(pc -> pc.active() && pc.code().equalsIgnoreCase(code));
        if (activeDuplicate) {
            throw new IllegalArgumentException("CODE_EXISTS");
        }
        // Anti-farming guard defaults ON: omitting the field must never create a code
        // that a paying customer can re-redeem cycle after cycle.
        boolean firstTimeOnly = !Boolean.FALSE.equals(request.firstTimeOnly());
        PromoCode created = stripeService.createPromotionCode(code, request.percentOff(),
                duration, request.durationInMonths(), firstTimeOnly);
        return new InfluencerCodeDTO(created.id(), created.code(), created.active(),
                created.percentOff(), created.duration(), created.durationInMonths(),
                created.firstTimeOnly(), created.timesRedeemed(), 0, 0, 0);
    }

    /** Deactivates a code so it can no longer be redeemed (history is preserved). */
    public void deactivate(String promotionCodeId) throws StripeException {
        stripeService.deactivatePromotionCode(promotionCodeId);
    }

    /**
     * Pure merge of the two attribution sources. For each Stripe code: signups are the
     * users whose ?ref code matches it (case-insensitive), conversions are the users the
     * webhook tied to it by id, and activePremium counts conversions still premium.
     * Ref codes with no matching Stripe code get their own Stripe-less rows, so a plain
     * link campaign is tracked too. Rows come out alphabetically by code.
     */
    static List<InfluencerCodeDTO> merge(List<PromoCode> stripeCodes, List<User> refUsers,
            List<User> promoUsers) {
        List<InfluencerCodeDTO> rows = new ArrayList<>();
        for (PromoCode pc : stripeCodes) {
            long signups = refUsers.stream()
                    .filter(u -> pc.code().equalsIgnoreCase(u.getReferralSource())).count();
            List<User> conversions = promoUsers.stream()
                    .filter(u -> pc.id().equals(u.getStripePromotionCodeId())).toList();
            long activePremium = conversions.stream().filter(User::isPremiumActive).count();
            rows.add(new InfluencerCodeDTO(pc.id(), pc.code(), pc.active(), pc.percentOff(),
                    pc.duration(), pc.durationInMonths(), pc.firstTimeOnly(),
                    pc.timesRedeemed(), signups, conversions.size(), activePremium));
        }
        // Ref-only codes (no Stripe discount behind them), grouped case-insensitively.
        TreeMap<String, Long> refOnly = new TreeMap<>();
        for (User user : refUsers) {
            String code = user.getReferralSource().toUpperCase(Locale.ROOT);
            boolean coveredByStripe = stripeCodes.stream()
                    .anyMatch(pc -> pc.code().equalsIgnoreCase(code));
            if (!coveredByStripe) {
                refOnly.merge(code, 1L, Long::sum);
            }
        }
        refOnly.forEach((code, signups) -> rows.add(new InfluencerCodeDTO(
                null, code, null, null, null, null, null, null, signups, 0, 0)));
        rows.sort((a, b) -> a.code().compareToIgnoreCase(b.code()));
        return rows;
    }

    // ————————————————————————— Settlements —————————————————————————

    /**
     * The commission settlement for a date range (inclusive), computed on demand from
     * the immutable payment ledger — any past period is reproducible at any time,
     * always with the same result. Stripe is only consulted to translate promotion
     * code ids into their human-readable codes; if that fails the raw ids are shown,
     * so settlements keep working even with billing down.
     */
    public InfluencerSettlementsDTO settlements(LocalDate from, LocalDate to) {
        List<PromoCode> stripeCodes = List.of();
        if (stripeService.isConfigured()) {
            try {
                stripeCodes = stripeService.listPromotionCodes();
            } catch (StripeException e) {
                log.warn("Stripe code listing failed; settlements will show raw promo ids", e);
            }
        }
        return new InfluencerSettlementsDTO(from, to, payoutMonthlyCents, payoutYearlyCents,
                settle(paymentRepository.findAll(), stripeCodes, from, to,
                        payoutMonthlyCents, payoutYearlyCents));
    }

    /**
     * Pure settlement computation over the payment ledger. Per influencer code and per
     * customer within [from, to]:
     *
     * <ul>
     *   <li><b>new</b> — the customer's first-ever charge falls inside the range.</li>
     *   <li><b>renewal</b> — charged inside the range, but was already a customer.</li>
     *   <li><b>churned</b> — no charge in range and the paid coverage ran out inside it.</li>
     *   <li><b>active</b> — no charge in range but still covered past its end (e.g. a
     *       yearly customer mid-period) — never a false churn.</li>
     * </ul>
     *
     * The payout is per CHARGE in range (monthly/yearly rate), so a customer who is
     * charged every month generates commission every month — while a charge can never
     * be paid twice because each ledger row is unique per Stripe invoice.
     */
    static List<InfluencerSettlementRowDTO> settle(List<InfluencerPayment> payments,
            List<PromoCode> stripeCodes, LocalDate from, LocalDate to,
            long monthlyCents, long yearlyCents) {
        Map<String, String> codeById = new HashMap<>();
        for (PromoCode pc : stripeCodes) {
            codeById.put(pc.id(), pc.code().toUpperCase(Locale.ROOT));
        }

        // influencer code → customer id → that customer's charges (all time)
        Map<String, Map<Long, List<InfluencerPayment>>> byCode = new TreeMap<>();
        for (InfluencerPayment p : payments) {
            String key = p.getPromotionCodeId() != null && !p.getPromotionCodeId().isBlank()
                    ? codeById.getOrDefault(p.getPromotionCodeId(), p.getPromotionCodeId())
                    : p.getReferralSource().toUpperCase(Locale.ROOT);
            byCode.computeIfAbsent(key, k -> new LinkedHashMap<>())
                    .computeIfAbsent(p.getUserId(), k -> new ArrayList<>()).add(p);
        }

        List<InfluencerSettlementRowDTO> rows = new ArrayList<>();
        for (Map.Entry<String, Map<Long, List<InfluencerPayment>>> codeEntry : byCode.entrySet()) {
            int newCustomers = 0, renewals = 0, churned = 0, active = 0;
            int monthlyCharges = 0, yearlyCharges = 0;
            long payout = 0;
            List<InfluencerSettlementCustomerDTO> customers = new ArrayList<>();

            for (Map.Entry<Long, List<InfluencerPayment>> customerEntry : codeEntry.getValue().entrySet()) {
                List<InfluencerPayment> all = new ArrayList<>(customerEntry.getValue());
                all.sort(Comparator.comparing(InfluencerPayment::getPaidOn));
                InfluencerPayment latest = all.get(all.size() - 1);
                LocalDate first = all.get(0).getPaidOn();
                LocalDate covered = all.stream().map(InfluencerPayment::getCoveredUntil)
                        .max(Comparator.naturalOrder()).orElse(first);
                List<InfluencerPayment> inRange = all.stream()
                        .filter(p -> !p.getPaidOn().isBefore(from) && !p.getPaidOn().isAfter(to))
                        .toList();
                boolean coveredThrough = covered.isAfter(to);

                String status;
                if (!inRange.isEmpty()) {
                    status = !first.isBefore(from) ? "new" : "renewal";
                } else if (!covered.isBefore(from) && !covered.isAfter(to)) {
                    status = "churned";
                } else if (coveredThrough && first.isBefore(from)) {
                    status = "active";
                } else {
                    continue; // not a customer within this window
                }

                long customerPayout = 0;
                int monthly = 0, yearly = 0;
                for (InfluencerPayment p : inRange) {
                    if ("yearly".equals(p.getPlan())) {
                        yearly++;
                        customerPayout += yearlyCents;
                    } else {
                        monthly++;
                        customerPayout += monthlyCents;
                    }
                }

                switch (status) {
                    case "new" -> newCustomers++;
                    case "renewal" -> renewals++;
                    case "churned" -> churned++;
                    default -> { }
                }
                if (coveredThrough) {
                    active++;
                }
                monthlyCharges += monthly;
                yearlyCharges += yearly;
                payout += customerPayout;
                customers.add(new InfluencerSettlementCustomerDTO(customerEntry.getKey(),
                        latest.getUsername(), latest.getPlan(), status, inRange.size(),
                        customerPayout, first, latest.getPaidOn(), covered));
            }

            if (customers.isEmpty()) {
                continue;
            }
            customers.sort(Comparator.comparing(InfluencerSettlementCustomerDTO::firstPaidOn));
            rows.add(new InfluencerSettlementRowDTO(codeEntry.getKey(), newCustomers, renewals,
                    churned, active, monthlyCharges, yearlyCharges, payout, customers));
        }
        return rows;
    }
}
