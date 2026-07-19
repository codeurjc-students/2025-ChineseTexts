package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.chinesereads.backend.Model.InfluencerPayment;
import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.InfluencerPaymentRepository;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.InfluencerService;
import com.chinesereads.backend.Service.StripeService;
import com.chinesereads.backend.Service.StripeService.PromoCode;
import com.chinesereads.backend.dto.InfluencerCodeDTO;
import com.chinesereads.backend.dto.InfluencerCreateDTO;
import com.chinesereads.backend.dto.InfluencerSettlementRowDTO;

/**
 * Influencer tracking: merging Stripe promotion codes with the locally-attributed
 * signups/conversions, and the validation gate on code creation. Stripe I/O is mocked —
 * only the pure merge/validation logic runs here.
 */
public class InfluencerServiceTest {

    private StripeService stripeService;
    private UserRepository userRepository;
    private InfluencerPaymentRepository paymentRepository;
    private InfluencerService influencerService;

    @BeforeEach
    public void setUp() {
        stripeService = mock(StripeService.class);
        userRepository = mock(UserRepository.class);
        paymentRepository = mock(InfluencerPaymentRepository.class);
        influencerService = new InfluencerService(stripeService, userRepository,
                paymentRepository, 200L, 1800L);
    }

    private User refUser(String ref) {
        User user = new User(ref + "@t.com", "U", "pass", "en", "USER");
        user.setReferralSource(ref);
        return user;
    }

    private User promoUser(String promoId, boolean premium) {
        User user = new User(promoId + "@t.com", "U", "pass", "en", "USER");
        user.setStripePromotionCodeId(promoId);
        if (premium) {
            user.setPremiumUntil(LocalDateTime.now().plusDays(10));
        }
        return user;
    }

    @Test
    @DisplayName("Stats join a Stripe code with its signups, conversions and active premiums")
    public void testStatsJoinSources() throws Exception {
        when(stripeService.listPromotionCodes()).thenReturn(List.of(
                new PromoCode("promo_1", "MARIA", true, 20L, "once", null, 3L, true)));
        // Signup matching is case-insensitive: both ref spellings count for MARIA.
        when(userRepository.findByReferralSourceIsNotNull())
                .thenReturn(List.of(refUser("MARIA"), refUser("maria")));
        when(userRepository.findByStripePromotionCodeIdIsNotNull())
                .thenReturn(List.of(promoUser("promo_1", true), promoUser("promo_1", false)));

        List<InfluencerCodeDTO> stats = influencerService.getStats();

        assertEquals(1, stats.size());
        InfluencerCodeDTO row = stats.get(0);
        assertEquals("MARIA", row.code());
        assertEquals(20L, row.percentOff());
        assertEquals(3L, row.timesRedeemed());
        assertEquals(2, row.signups());
        assertEquals(2, row.conversions());
        assertEquals(1, row.activePremium());
    }

    @Test
    @DisplayName("A ?ref code with no Stripe discount behind it still gets its own row")
    public void testStatsIncludeRefOnlyCodes() throws Exception {
        when(stripeService.listPromotionCodes()).thenReturn(List.of(
                new PromoCode("promo_1", "MARIA", true, 20L, "once", null, 0L, true)));
        when(userRepository.findByReferralSourceIsNotNull())
                .thenReturn(List.of(refUser("BLOG"), refUser("blog"), refUser("MARIA")));
        when(userRepository.findByStripePromotionCodeIdIsNotNull()).thenReturn(List.of());

        List<InfluencerCodeDTO> stats = influencerService.getStats();

        assertEquals(2, stats.size()); // alphabetical: BLOG, MARIA
        InfluencerCodeDTO blog = stats.get(0);
        assertEquals("BLOG", blog.code());
        assertNull(blog.id()); // no Stripe code behind it
        assertEquals(2, blog.signups());
        assertEquals("MARIA", stats.get(1).code());
        assertEquals(1, stats.get(1).signups());
    }

    @Test
    @DisplayName("Creating a code normalizes it to upper case and returns the new row")
    public void testCreateNormalizesCode() throws Exception {
        when(stripeService.listPromotionCodes()).thenReturn(List.of());
        when(stripeService.createPromotionCode(anyString(), anyLong(), anyString(), any(), anyBoolean()))
                .thenReturn(new PromoCode("promo_9", "MARIA20", true, 15L, "once", null, 0L, true));

        InfluencerCodeDTO created = influencerService.create(
                new InfluencerCreateDTO("maria20", 15L, "once", null, null));

        assertEquals("MARIA20", created.code());
        assertEquals(0, created.signups());
        verify(stripeService).createPromotionCode("MARIA20", 15L, "once", null, true);
    }

    @Test
    @DisplayName("Invalid inputs are rejected before any Stripe call is made")
    public void testCreateValidation() throws Exception {
        when(stripeService.listPromotionCodes()).thenReturn(List.of(
                new PromoCode("promo_1", "TAKEN", true, 10L, "once", null, 0L, true)));

        assertThrows(IllegalArgumentException.class, () -> influencerService.create(
                new InfluencerCreateDTO("a!", 10L, "once", null, null)));         // bad charset/length
        assertThrows(IllegalArgumentException.class, () -> influencerService.create(
                new InfluencerCreateDTO("VALIDO", 0L, "once", null, null)));      // percent out of range
        assertThrows(IllegalArgumentException.class, () -> influencerService.create(
                new InfluencerCreateDTO("VALIDO", 10L, "weekly", null, null)));   // unknown duration
        assertThrows(IllegalArgumentException.class, () -> influencerService.create(
                new InfluencerCreateDTO("VALIDO", 10L, "repeating", null, null))); // months missing
        assertThrows(IllegalArgumentException.class, () -> influencerService.create(
                new InfluencerCreateDTO("taken", 10L, "once", null, null)));      // active duplicate

        verify(stripeService, never()).createPromotionCode(anyString(), anyLong(), anyString(), any(), anyBoolean());
    }

    @Test
    @DisplayName("Anti-farming guard: omitting firstTimeOnly creates a first-time-only code")
    public void testCreateDefaultsToFirstTimeOnly() throws Exception {
        when(stripeService.listPromotionCodes()).thenReturn(List.of());
        when(stripeService.createPromotionCode(anyString(), anyLong(), anyString(), any(), anyBoolean()))
                .thenReturn(new PromoCode("promo_9", "MARIA20", true, 15L, "once", null, 0L, true));

        influencerService.create(new InfluencerCreateDTO("MARIA20", 15L, "once", null, null));

        // Null (field omitted by an older client) must still land as TRUE in Stripe.
        verify(stripeService).createPromotionCode("MARIA20", 15L, "once", null, true);
    }

    @Test
    @DisplayName("Win-back campaigns: an explicit firstTimeOnly=false is passed through")
    public void testCreateAllowsExplicitWinBackCode() throws Exception {
        when(stripeService.listPromotionCodes()).thenReturn(List.of());
        when(stripeService.createPromotionCode(anyString(), anyLong(), anyString(), any(), anyBoolean()))
                .thenReturn(new PromoCode("promo_9", "VUELVE10", true, 10L, "once", null, 0L, false));

        InfluencerCodeDTO created = influencerService.create(
                new InfluencerCreateDTO("VUELVE10", 10L, "once", null, false));

        verify(stripeService).createPromotionCode("VUELVE10", 10L, "once", null, false);
        assertEquals(false, created.firstTimeOnly());
    }

    // ————————————————————— Settlements —————————————————————

    private InfluencerPayment payment(long userId, String promoId, String ref, String plan,
            long amountCents, String invoiceId, LocalDate paidOn, LocalDate coveredUntil) {
        return new InfluencerPayment(userId, "User" + userId, promoId, ref, plan,
                amountCents, invoiceId, paidOn, coveredUntil);
    }

    @Test
    @DisplayName("A month's settlement classifies new / renewal / churned / active correctly")
    public void testSettlementStatuses() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);
        when(stripeService.isConfigured()).thenReturn(false);
        when(paymentRepository.findAll()).thenReturn(List.of(
                // C1: paid June AND July → renewal in July, covered past July → active.
                payment(1, null, "maria", "monthly", 699, "in_1",
                        LocalDate.of(2026, 6, 5), LocalDate.of(2026, 7, 5)),
                payment(1, null, "maria", "monthly", 699, "in_2",
                        LocalDate.of(2026, 7, 5), LocalDate.of(2026, 8, 5)),
                // C2: first-ever charge inside July → new.
                payment(2, null, "maria", "monthly", 699, "in_3",
                        LocalDate.of(2026, 7, 10), LocalDate.of(2026, 8, 10)),
                // C3: paid June only; coverage ran out July 20 → churned (no commission).
                payment(3, null, "maria", "monthly", 699, "in_4",
                        LocalDate.of(2026, 6, 20), LocalDate.of(2026, 7, 20)),
                // C4: yearly customer mid-period → active, NEVER a false churn.
                payment(4, null, "maria", "yearly", 5999, "in_5",
                        LocalDate.of(2026, 2, 1), LocalDate.of(2027, 2, 1))));

        List<InfluencerSettlementRowDTO> rows =
                influencerService.settlements(from, to).rows();

        assertEquals(1, rows.size());
        InfluencerSettlementRowDTO row = rows.get(0);
        assertEquals("MARIA", row.code());
        assertEquals(1, row.newCustomers());
        assertEquals(1, row.renewals());
        assertEquals(1, row.churned());
        assertEquals(3, row.active()); // C1 + C2 + C4 covered past July 31
        assertEquals(2, row.monthlyCharges()); // C1 July + C2 July
        assertEquals(0, row.yearlyCharges());
        assertEquals(400L, row.payoutCents()); // 2 × €2 — churn and mid-year cost nothing
        assertEquals(4, row.customers().size());
    }

    @Test
    @DisplayName("Promo-id charges and ?ref charges of the same influencer merge into one row")
    public void testSettlementMergesPromoAndRefAttribution() throws Exception {
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.listPromotionCodes()).thenReturn(List.of(
                new PromoCode("promo_1", "MARIA", true, 20L, "once", null, 1L, true)));
        when(paymentRepository.findAll()).thenReturn(List.of(
                payment(1, "promo_1", null, "monthly", 699, "in_1",
                        LocalDate.of(2026, 7, 3), LocalDate.of(2026, 8, 3)),
                payment(2, null, "maria", "monthly", 699, "in_2",
                        LocalDate.of(2026, 7, 4), LocalDate.of(2026, 8, 4)),
                // A code deleted from Stripe still settles, under its raw promo id.
                payment(3, "promo_gone", null, "monthly", 699, "in_3",
                        LocalDate.of(2026, 7, 5), LocalDate.of(2026, 8, 5))));

        List<InfluencerSettlementRowDTO> rows = influencerService
                .settlements(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)).rows();

        assertEquals(2, rows.size());
        InfluencerSettlementRowDTO maria = rows.stream()
                .filter(r -> r.code().equals("MARIA")).findFirst().orElseThrow();
        assertEquals(2, maria.newCustomers());
        assertEquals(400L, maria.payoutCents());
        assertTrue(rows.stream().anyMatch(r -> r.code().equals("promo_gone")));
    }

    @Test
    @DisplayName("A yearly charge pays the yearly commission; a day-range only counts its days")
    public void testSettlementYearlyPayoutAndDayRange() {
        when(stripeService.isConfigured()).thenReturn(false);
        when(paymentRepository.findAll()).thenReturn(List.of(
                payment(1, null, "blog", "yearly", 5999, "in_1",
                        LocalDate.of(2026, 7, 15), LocalDate.of(2027, 7, 15)),
                payment(2, null, "blog", "monthly", 699, "in_2",
                        LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 20))));

        // Custom day range covering only the yearly charge.
        List<InfluencerSettlementRowDTO> rows = influencerService
                .settlements(LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 15)).rows();

        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).yearlyCharges());
        assertEquals(0, rows.get(0).monthlyCharges());
        assertEquals(1800L, rows.get(0).payoutCents());
        assertEquals(1, rows.get(0).customers().size()); // the July-20 customer is outside
    }

    @Test
    @DisplayName("The string of a DEACTIVATED code can be reused for a new campaign")
    public void testCreateAllowsReusingInactiveCode() throws Exception {
        when(stripeService.listPromotionCodes()).thenReturn(List.of(
                new PromoCode("promo_old", "MARIA", false, 10L, "once", null, 5L, true)));
        when(stripeService.createPromotionCode(anyString(), anyLong(), anyString(), any(), anyBoolean()))
                .thenReturn(new PromoCode("promo_new", "MARIA", true, 25L, "once", null, 0L, true));

        InfluencerCodeDTO created = influencerService.create(
                new InfluencerCreateDTO("MARIA", 25L, "once", null, null));

        assertTrue(created.active());
        assertEquals("promo_new", created.id());
    }
}
