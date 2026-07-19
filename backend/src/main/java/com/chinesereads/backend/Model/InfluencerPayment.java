package com.chinesereads.backend.Model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One row per Stripe charge actually COLLECTED from an influencer-attributed customer:
 * the append-only ledger behind influencer settlements (€X per monthly charge, €Y per
 * yearly charge). Written by the Stripe webhook; never updated or deleted.
 *
 * <p>Design decisions that make settlements trustworthy:
 * <ul>
 *   <li>{@code invoiceId} is UNIQUE — Stripe retries webhooks, but the same charge can
 *       never be counted (and therefore paid to an influencer) twice.</li>
 *   <li>No foreign key to {@link User}: the plain {@code userId} plus a {@code username}
 *       snapshot survive account deletion, so a past month's settlement never changes
 *       retroactively.</li>
 *   <li>Rows are only written when money was collected (amount &gt; 0), so every euro of
 *       commission maps to a euro already in the account.</li>
 * </ul>
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "invoiceId"))
public class InfluencerPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /** Plain id (no FK) so the ledger survives account deletion. */
    private Long userId;

    /** Display-name snapshot taken at charge time (kept after account deletion). */
    private String username;

    /** Stripe promotion code id ({@code promo_...}) the customer redeemed, if any. */
    private String promotionCodeId;

    /** The customer's ?ref signup code, if any (attribution when no promo was used). */
    private String referralSource;

    /** "monthly" or "yearly" — decides the per-charge commission. */
    private String plan;

    /** Amount actually collected by Stripe for this charge, in cents. */
    private long amountCents;

    /** Stripe invoice id of the charge — the uniqueness (anti-double-count) key. */
    private String invoiceId;

    private LocalDate paidOn;

    /** End of the period this charge pays for (drives active/churned computations). */
    private LocalDate coveredUntil;

    public InfluencerPayment() {
    }

    public InfluencerPayment(Long userId, String username, String promotionCodeId,
            String referralSource, String plan, long amountCents, String invoiceId,
            LocalDate paidOn, LocalDate coveredUntil) {
        this.userId = userId;
        this.username = username;
        this.promotionCodeId = promotionCodeId;
        this.referralSource = referralSource;
        this.plan = plan;
        this.amountCents = amountCents;
        this.invoiceId = invoiceId;
        this.paidOn = paidOn;
        this.coveredUntil = coveredUntil;
    }

    public long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPromotionCodeId() {
        return promotionCodeId;
    }

    public String getReferralSource() {
        return referralSource;
    }

    public String getPlan() {
        return plan;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public LocalDate getPaidOn() {
        return paidOn;
    }

    public LocalDate getCoveredUntil() {
        return coveredUntil;
    }
}
