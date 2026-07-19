package com.chinesereads.backend.Service;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chinesereads.backend.Model.InfluencerPayment;
import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.InfluencerPaymentRepository;

/**
 * Writes the influencer commission ledger ({@link InfluencerPayment}) from the Stripe
 * webhook. Lives apart from {@link InfluencerService} (which reads the ledger and also
 * depends on StripeService) so StripeService can inject it without a dependency cycle.
 *
 * <p>Recording rules — all of them protect the business from over-paying commissions:
 * only charges with money actually collected, only customers attributed to an
 * influencer (promo code or ?ref), and never the same Stripe invoice twice (checked
 * up front and enforced by the DB unique constraint against webhook-retry races).
 */
@Service
public class InfluencerPaymentService {

    private static final Logger log = LoggerFactory.getLogger(InfluencerPaymentService.class);

    private final InfluencerPaymentRepository repository;

    public InfluencerPaymentService(InfluencerPaymentRepository repository) {
        this.repository = repository;
    }

    /**
     * Records one collected charge for an influencer-attributed customer. Silently
     * skips anything that must not generate commission; a skip is never an error —
     * the webhook must always return 200 so Stripe does not retry forever.
     *
     * @param coveredUntil end of the paid period; when Stripe did not provide one it
     *        falls back to paidOn + 1 month/year according to the plan.
     */
    @Transactional
    public void record(User user, String invoiceId, String plan, long amountCents,
            LocalDate paidOn, LocalDate coveredUntil) {
        if (user == null || invoiceId == null || invoiceId.isBlank()) {
            return;
        }
        if (amountCents <= 0) {
            return; // no money collected (e.g. 100% coupon) → no commission
        }
        boolean attributed = (user.getStripePromotionCodeId() != null
                && !user.getStripePromotionCodeId().isBlank())
                || (user.getReferralSource() != null && !user.getReferralSource().isBlank());
        if (!attributed) {
            return; // organic customer — nothing to settle
        }
        if (repository.existsByInvoiceId(invoiceId)) {
            return; // webhook retry / duplicate event — already counted
        }
        LocalDate paid = paidOn != null ? paidOn : LocalDate.now();
        LocalDate covered = coveredUntil != null ? coveredUntil
                : ("yearly".equals(plan) ? paid.plusYears(1) : paid.plusMonths(1));
        try {
            repository.save(new InfluencerPayment(user.getId(), user.getName(),
                    user.getStripePromotionCodeId(), user.getReferralSource(),
                    plan, amountCents, invoiceId, paid, covered));
        } catch (DataIntegrityViolationException e) {
            // Concurrent webhook retry hit the unique constraint — the charge is
            // already recorded, which is exactly what we want.
            log.debug("Invoice {} already recorded (concurrent retry)", invoiceId);
        }
    }
}
