package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.StripeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests the subscription-to-user reconciliation logic in isolation from the Stripe SDK.
 * The webhook dispatch extracts plain primitives from Stripe objects and delegates to
 * {@link StripeService#applySubscription} / {@link StripeService#revokeSubscription},
 * which are the methods exercised here.
 */
public class StripeServiceTest {

    private UserRepository userRepository;
    private StripeService stripeService;

    @BeforeEach
    public void setUp() {
        userRepository = mock(UserRepository.class);
        stripeService = new StripeService();
        injectField(stripeService, "userRepository", userRepository);
        injectField(stripeService, "priceMonthly", "price_month");
        injectField(stripeService, "priceYearly", "price_year");
    }

    private long futureEpoch() {
        return Instant.now().plusSeconds(30L * 24 * 3600).getEpochSecond();
    }

    @Test
    @DisplayName("Maps the plan name to the configured Stripe price id")
    public void testPriceIdFor() {
        assertEquals("price_month", stripeService.priceIdFor("monthly"));
        assertEquals("price_year", stripeService.priceIdFor("yearly"));
        assertEquals("price_month", stripeService.priceIdFor("something-else")); // defaults to monthly
    }

    @Test
    @DisplayName("A checkout (client reference) grants premium and stores the Stripe ids")
    public void testApplyByClientReference() {
        User user = new User("a@a.com", "A", "pass", "en", "USER");
        user.setId(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        stripeService.applySubscription("7", "cus_1", "sub_1", futureEpoch());

        assertEquals("cus_1", user.getStripeCustomerId());
        assertEquals("sub_1", user.getStripeSubscriptionId());
        assertTrue(user.isPremiumActive());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("A renewal (no client reference) reconciles by Stripe customer id")
    public void testApplyByCustomerId() {
        User user = new User("b@b.com", "B", "pass", "en", "USER");
        user.setStripeCustomerId("cus_2");
        when(userRepository.findByStripeCustomerId("cus_2")).thenReturn(Optional.of(user));

        stripeService.applySubscription(null, "cus_2", "sub_2", futureEpoch());

        assertEquals("sub_2", user.getStripeSubscriptionId());
        assertTrue(user.isPremiumActive());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("An event that matches no user is ignored (no save)")
    public void testNoMatchIsIgnored() {
        stripeService.applySubscription("999", "cus_x", "sub_x", futureEpoch());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cancellation detaches the subscription but leaves paid time to lapse")
    public void testRevokeKeepsPaidTime() {
        User user = new User("c@c.com", "C", "pass", "en", "USER");
        user.setStripeSubscriptionId("sub_3");
        user.setPremiumUntil(LocalDateTime.now().plusDays(10)); // already-paid time remaining
        when(userRepository.findByStripeSubscriptionId("sub_3")).thenReturn(Optional.of(user));

        stripeService.revokeSubscription("sub_3");

        assertNull(user.getStripeSubscriptionId());
        assertTrue(user.isPremiumActive()); // premiumUntil untouched — runs out naturally
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("A subscription.updated event grants premium from the payload's period end")
    public void testDispatchSubscriptionUpdated() throws Exception {
        User user = new User("d@d.com", "D", "pass", "en", "USER");
        user.setStripeCustomerId("cus_9");
        when(userRepository.findByStripeCustomerId("cus_9")).thenReturn(Optional.of(user));

        long periodEnd = futureEpoch();
        // Period end lives on the subscription item in the current Stripe API.
        String json = "{\"id\":\"sub_9\",\"customer\":\"cus_9\",\"status\":\"active\","
                + "\"items\":{\"data\":[{\"current_period_end\":" + periodEnd + "}]}}";
        JsonNode data = new ObjectMapper().readTree(json);

        stripeService.dispatchEvent("customer.subscription.updated", data);

        assertEquals("sub_9", user.getStripeSubscriptionId());
        assertTrue(user.isPremiumActive());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("A subscription.deleted event detaches the subscription")
    public void testDispatchSubscriptionDeleted() throws Exception {
        User user = new User("e@e.com", "E", "pass", "en", "USER");
        user.setStripeSubscriptionId("sub_10");
        user.setPremiumUntil(LocalDateTime.now().plusDays(5));
        when(userRepository.findByStripeSubscriptionId("sub_10")).thenReturn(Optional.of(user));

        JsonNode data = new ObjectMapper().readTree("{\"id\":\"sub_10\",\"status\":\"canceled\"}");

        stripeService.dispatchEvent("customer.subscription.deleted", data);

        assertNull(user.getStripeSubscriptionId());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Not configured until a Stripe secret key is present")
    public void testIsConfigured() {
        assertFalse(stripeService.isConfigured());
        injectField(stripeService, "secretKey", "sk_test_123");
        assertTrue(stripeService.isConfigured());
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
