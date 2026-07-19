package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.chinesereads.backend.Model.InfluencerPayment;
import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.InfluencerPaymentRepository;
import com.chinesereads.backend.Service.InfluencerPaymentService;

/**
 * The commission-ledger write rules: only collected money, only influencer-attributed
 * customers, never the same Stripe invoice twice. These rules are what guarantee the
 * business can never over-pay an influencer.
 */
public class InfluencerPaymentServiceTest {

    private InfluencerPaymentRepository repository;
    private InfluencerPaymentService service;

    @BeforeEach
    public void setUp() {
        repository = mock(InfluencerPaymentRepository.class);
        service = new InfluencerPaymentService(repository);
    }

    private User attributedUser() {
        User user = new User("a@a.com", "Ana", "pass", "en", "USER");
        user.setId(5L);
        user.setStripePromotionCodeId("promo_maria");
        return user;
    }

    @Test
    @DisplayName("An attributed, collected charge is recorded with its full snapshot")
    public void testRecordsAttributedCharge() {
        when(repository.existsByInvoiceId("in_1")).thenReturn(false);

        service.record(attributedUser(), "in_1", "monthly", 699,
                LocalDate.of(2026, 7, 5), LocalDate.of(2026, 8, 5));

        ArgumentCaptor<InfluencerPayment> captor = ArgumentCaptor.forClass(InfluencerPayment.class);
        verify(repository).save(captor.capture());
        InfluencerPayment saved = captor.getValue();
        assertEquals(5L, saved.getUserId());
        assertEquals("Ana", saved.getUsername());
        assertEquals("promo_maria", saved.getPromotionCodeId());
        assertEquals("monthly", saved.getPlan());
        assertEquals(699L, saved.getAmountCents());
        assertEquals("in_1", saved.getInvoiceId());
        assertEquals(LocalDate.of(2026, 8, 5), saved.getCoveredUntil());
    }

    @Test
    @DisplayName("A ?ref-only customer (no promo code at checkout) is still recorded")
    public void testRecordsRefOnlyCustomer() {
        User user = new User("b@b.com", "Bea", "pass", "en", "USER");
        user.setReferralSource("MARIA10");
        when(repository.existsByInvoiceId(anyString())).thenReturn(false);

        service.record(user, "in_2", "monthly", 699, LocalDate.now(), null);

        verify(repository).save(any());
    }

    @Test
    @DisplayName("An organic customer (no attribution at all) never enters the ledger")
    public void testSkipsOrganicCustomer() {
        User user = new User("c@c.com", "Cai", "pass", "en", "USER");

        service.record(user, "in_3", "monthly", 699, LocalDate.now(), null);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("A charge with no money collected (e.g. 100% coupon) pays no commission")
    public void testSkipsZeroAmount() {
        service.record(attributedUser(), "in_4", "monthly", 0, LocalDate.now(), null);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("The same Stripe invoice can never be recorded (and paid) twice")
    public void testSkipsDuplicateInvoice() {
        when(repository.existsByInvoiceId("in_5")).thenReturn(true);

        service.record(attributedUser(), "in_5", "monthly", 699, LocalDate.now(), null);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("A missing coveredUntil falls back to paidOn + 1 month / 1 year by plan")
    public void testCoveredUntilFallback() {
        when(repository.existsByInvoiceId(anyString())).thenReturn(false);
        ArgumentCaptor<InfluencerPayment> captor = ArgumentCaptor.forClass(InfluencerPayment.class);

        service.record(attributedUser(), "in_6", "yearly", 5999, LocalDate.of(2026, 7, 1), null);

        verify(repository).save(captor.capture());
        assertEquals(LocalDate.of(2027, 7, 1), captor.getValue().getCoveredUntil());
    }

    @Test
    @DisplayName("Null user or blank invoice id is silently skipped (webhook stays 200)")
    public void testSkipsNullInputs() {
        service.record(null, "in_7", "monthly", 699, LocalDate.now(), null);
        service.record(attributedUser(), " ", "monthly", 699, LocalDate.now(), null);
        verify(repository, never()).save(any());
    }
}
