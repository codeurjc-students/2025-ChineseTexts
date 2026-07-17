package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.InfluencerService;
import com.chinesereads.backend.Service.StripeService;
import com.chinesereads.backend.Service.StripeService.PromoCode;
import com.chinesereads.backend.dto.InfluencerCodeDTO;
import com.chinesereads.backend.dto.InfluencerCreateDTO;

/**
 * Influencer tracking: merging Stripe promotion codes with the locally-attributed
 * signups/conversions, and the validation gate on code creation. Stripe I/O is mocked —
 * only the pure merge/validation logic runs here.
 */
public class InfluencerServiceTest {

    private StripeService stripeService;
    private UserRepository userRepository;
    private InfluencerService influencerService;

    @BeforeEach
    public void setUp() {
        stripeService = mock(StripeService.class);
        userRepository = mock(UserRepository.class);
        influencerService = new InfluencerService(stripeService, userRepository);
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
                new PromoCode("promo_1", "MARIA", true, 20L, "once", null, 3L)));
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
                new PromoCode("promo_1", "MARIA", true, 20L, "once", null, 0L)));
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
        when(stripeService.createPromotionCode(anyString(), anyLong(), anyString(), any()))
                .thenReturn(new PromoCode("promo_9", "MARIA20", true, 15L, "once", null, 0L));

        InfluencerCodeDTO created = influencerService.create(
                new InfluencerCreateDTO("maria20", 15L, "once", null));

        assertEquals("MARIA20", created.code());
        assertEquals(0, created.signups());
        verify(stripeService).createPromotionCode("MARIA20", 15L, "once", null);
    }

    @Test
    @DisplayName("Invalid inputs are rejected before any Stripe call is made")
    public void testCreateValidation() throws Exception {
        when(stripeService.listPromotionCodes()).thenReturn(List.of(
                new PromoCode("promo_1", "TAKEN", true, 10L, "once", null, 0L)));

        assertThrows(IllegalArgumentException.class, () -> influencerService.create(
                new InfluencerCreateDTO("a!", 10L, "once", null)));         // bad charset/length
        assertThrows(IllegalArgumentException.class, () -> influencerService.create(
                new InfluencerCreateDTO("VALIDO", 0L, "once", null)));      // percent out of range
        assertThrows(IllegalArgumentException.class, () -> influencerService.create(
                new InfluencerCreateDTO("VALIDO", 10L, "weekly", null)));   // unknown duration
        assertThrows(IllegalArgumentException.class, () -> influencerService.create(
                new InfluencerCreateDTO("VALIDO", 10L, "repeating", null))); // months missing
        assertThrows(IllegalArgumentException.class, () -> influencerService.create(
                new InfluencerCreateDTO("taken", 10L, "once", null)));      // active duplicate

        verify(stripeService, never()).createPromotionCode(anyString(), anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("The string of a DEACTIVATED code can be reused for a new campaign")
    public void testCreateAllowsReusingInactiveCode() throws Exception {
        when(stripeService.listPromotionCodes()).thenReturn(List.of(
                new PromoCode("promo_old", "MARIA", false, 10L, "once", null, 5L)));
        when(stripeService.createPromotionCode(anyString(), anyLong(), anyString(), any()))
                .thenReturn(new PromoCode("promo_new", "MARIA", true, 25L, "once", null, 0L));

        InfluencerCodeDTO created = influencerService.create(
                new InfluencerCreateDTO("MARIA", 25L, "once", null));

        assertTrue(created.active());
        assertEquals("promo_new", created.id());
    }
}
