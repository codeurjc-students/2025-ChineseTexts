package com.chinesereads.backend.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.StripeService.PromoCode;
import com.chinesereads.backend.dto.InfluencerCodeDTO;
import com.chinesereads.backend.dto.InfluencerCreateDTO;
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

    /** Codes are shared verbally and in video captions: short, unambiguous charset. */
    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Za-z0-9_-]{3,40}");

    private final StripeService stripeService;
    private final UserRepository userRepository;

    public InfluencerService(StripeService stripeService, UserRepository userRepository) {
        this.stripeService = stripeService;
        this.userRepository = userRepository;
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
}
