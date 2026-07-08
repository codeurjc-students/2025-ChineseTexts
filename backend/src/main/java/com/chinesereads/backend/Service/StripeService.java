package com.chinesereads.backend.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

/**
 * Stripe integration for the PREMIUM subscription plan.
 *
 * <p>Checkout runs on Stripe's hosted page (redirect flow), so the app never sees card
 * data. When Stripe confirms a payment it calls {@link #handleWebhook}, which sets the
 * user's {@code premiumUntil} timestamp — the single source of truth for premium status
 * (shared with the admin-grant path from PR #1).
 *
 * <p>All Stripe credentials come from the environment and are empty by default, so the
 * app boots without billing configured; {@link #isConfigured()} gates the paid paths.
 */
@Service
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${stripe.secret-key:}")
    private String secretKey;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${stripe.price.monthly:}")
    private String priceMonthly;

    @Value("${stripe.price.yearly:}")
    private String priceYearly;

    @Value("${app.public-url:}")
    private String publicUrl;

    @Autowired
    private UserRepository userRepository;

    /** True only when a Stripe secret key is present, so billing endpoints can operate. */
    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }

    private void ensureApiKey() {
        Stripe.apiKey = secretKey;
    }

    /** Resolves the configured recurring Price ID for a plan ("monthly" / "yearly"). */
    public String priceIdFor(String plan) {
        return "yearly".equalsIgnoreCase(plan) ? priceYearly : priceMonthly;
    }

    // ————————————————————————— Checkout & portal —————————————————————————

    /**
     * Creates a subscription Checkout Session for the user and returns its redirect URL.
     * Ensures the user has a Stripe customer first so renewals reconcile to this account.
     */
    @Transactional
    public String createCheckoutSession(User user, String plan) throws StripeException {
        ensureApiKey();
        String priceId = priceIdFor(plan);
        if (priceId == null || priceId.isBlank()) {
            throw new IllegalArgumentException("No Stripe price configured for plan: " + plan);
        }

        String customerId = resolveOrCreateCustomer(user);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                // Lets the webhook map the payment back to this user even before we know
                // the subscription id.
                .setClientReferenceId(String.valueOf(user.getId()))
                .setSuccessUrl(publicUrl + "/premium/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(publicUrl + "/premium")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build())
                .build();

        return Session.create(params).getUrl();
    }

    /**
     * Returns a valid Stripe customer id for the user, creating a new one if needed.
     * A customer id stored under a different API mode (e.g. a test-mode customer after
     * switching to live keys) or a deleted customer no longer exists under the current
     * key, so it is treated as missing and replaced — otherwise Checkout would fail with
     * "no such customer".
     */
    private String resolveOrCreateCustomer(User user) throws StripeException {
        String customerId = user.getStripeCustomerId();
        if (customerId != null && !customerId.isBlank() && customerExists(customerId)) {
            return customerId;
        }
        Customer customer = Customer.create(CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(user.getName())
                .build());
        user.setStripeCustomerId(customer.getId());
        userRepository.save(user);
        return customer.getId();
    }

    /** True if the customer exists under the current API key and is not deleted. */
    private boolean customerExists(String customerId) {
        try {
            Customer customer = Customer.retrieve(customerId);
            return customer.getDeleted() == null || !customer.getDeleted();
        } catch (StripeException e) {
            return false; // unknown/invalid under the current key
        }
    }

    /**
     * Creates a Stripe Billing Portal session so the user can manage or cancel their
     * subscription. Returns {@code null} if the user has no valid Stripe customer.
     */
    public String createPortalSession(User user) throws StripeException {
        ensureApiKey();
        if (user.getStripeCustomerId() == null || user.getStripeCustomerId().isBlank()
                || !customerExists(user.getStripeCustomerId())) {
            return null;
        }
        com.stripe.model.billingportal.Session portal = com.stripe.model.billingportal.Session.create(
                com.stripe.param.billingportal.SessionCreateParams.builder()
                        .setCustomer(user.getStripeCustomerId())
                        .setReturnUrl(publicUrl + "/profile")
                        .build());
        return portal.getUrl();
    }

    // ————————————————————————————— Webhook —————————————————————————————

    /**
     * Verifies the Stripe signature and applies the relevant subscription events to the
     * matching user. Unknown event types are ignored.
     *
     * @throws com.stripe.exception.SignatureVerificationException if the signature is invalid
     */
    public void handleWebhook(String payload, String signatureHeader) throws StripeException {
        ensureApiKey();
        Event event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);

        // Read the event's data.object from the RAW JSON rather than the SDK's typed
        // deserializer: the field names we need (id, customer, subscription, status,
        // current_period_end) are stable across Stripe API versions, so this keeps the
        // webhook working even when the account's API version is newer than the SDK's.
        JsonNode data;
        try {
            data = MAPPER.readTree(event.getDataObjectDeserializer().getRawJson());
        } catch (Exception e) {
            log.warn("Could not parse Stripe event data for {}", event.getType(), e);
            return;
        }
        dispatchEvent(event.getType(), data);
    }

    /** Applies one parsed event to the matching user. Exposed for testing. */
    public void dispatchEvent(String type, JsonNode data) throws StripeException {
        switch (type) {
            // First payment: grant premium and record the Stripe ids.
            case "checkout.session.completed" -> {
                String subscriptionId = text(data, "subscription");
                applySubscription(text(data, "client_reference_id"), text(data, "customer"),
                        subscriptionId, periodEndOf(subscriptionId));
            }
            // Renewals AND status changes carry the subscription object (its period end
            // advances on each successful renewal) — read the period end straight from
            // the payload, no extra API call.
            case "customer.subscription.updated", "customer.subscription.created" -> {
                String status = text(data, "status");
                String subscriptionId = text(data, "id");
                if ("active".equals(status) || "trialing".equals(status)) {
                    applySubscription(null, text(data, "customer"), subscriptionId,
                            periodEndFromJson(data));
                } else if ("canceled".equals(status) || "unpaid".equals(status)) {
                    revokeSubscription(subscriptionId);
                }
            }
            case "customer.subscription.deleted" -> revokeSubscription(text(data, "id"));
            default -> log.debug("Ignoring Stripe event type {}", type);
        }
    }

    /** Retrieves a subscription by id and reads its current period end, or null. */
    private Long periodEndOf(String subscriptionId) throws StripeException {
        if (subscriptionId == null || subscriptionId.isBlank()) {
            return null;
        }
        Subscription subscription = Subscription.retrieve(subscriptionId);
        if (subscription.getItems() == null || subscription.getItems().getData() == null
                || subscription.getItems().getData().isEmpty()) {
            return null;
        }
        return subscription.getItems().getData().get(0).getCurrentPeriodEnd();
    }

    /**
     * Reads the subscription's current period end (epoch seconds) from the raw event
     * JSON. Tries the top-level field first (older API) then the subscription item
     * (current API), so it works regardless of the account's Stripe API version.
     */
    private Long periodEndFromJson(JsonNode subscription) {
        JsonNode top = subscription.get("current_period_end");
        if (top != null && top.isNumber()) {
            return top.asLong();
        }
        JsonNode items = subscription.path("items").path("data");
        if (items.isArray() && items.size() > 0) {
            JsonNode cpe = items.get(0).get("current_period_end");
            if (cpe != null && cpe.isNumber()) {
                return cpe.asLong();
            }
        }
        return null;
    }

    /** Null-safe string field read from a JSON node. */
    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }

    // ——— Pure state mutations (unit-tested without the Stripe SDK) ———

    /**
     * Grants/extends premium on the matching user: stores the Stripe ids and sets
     * {@code premiumUntil} to the subscription's period end. Idempotent — safe to call
     * on both the initial checkout and every renewal.
     */
    @Transactional
    public void applySubscription(String clientReferenceUserId, String customerId,
            String subscriptionId, Long periodEndEpochSeconds) {
        User user = resolveUser(clientReferenceUserId, customerId, subscriptionId);
        if (user == null) {
            log.warn("Stripe event could not be matched to a user (ref={}, customer={}, sub={})",
                    clientReferenceUserId, customerId, subscriptionId);
            return;
        }
        if (customerId != null && !customerId.isBlank()) {
            user.setStripeCustomerId(customerId);
        }
        if (subscriptionId != null && !subscriptionId.isBlank()) {
            user.setStripeSubscriptionId(subscriptionId);
        }
        if (periodEndEpochSeconds != null) {
            user.setPremiumUntil(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(periodEndEpochSeconds), ZoneId.systemDefault()));
        }
        userRepository.save(user);
    }

    /**
     * Detaches a cancelled subscription from its user. {@code premiumUntil} is left as-is
     * so already-paid time runs out naturally (the user keeps premium until period end).
     */
    @Transactional
    public void revokeSubscription(String subscriptionId) {
        if (subscriptionId == null) {
            return;
        }
        userRepository.findByStripeSubscriptionId(subscriptionId).ifPresent(user -> {
            user.setStripeSubscriptionId(null);
            userRepository.save(user);
        });
    }

    /** Finds the user behind an event, preferring the checkout reference, then the ids. */
    private User resolveUser(String clientReferenceUserId, String customerId, String subscriptionId) {
        if (clientReferenceUserId != null && !clientReferenceUserId.isBlank()) {
            try {
                User user = userRepository.findById(Long.parseLong(clientReferenceUserId)).orElse(null);
                if (user != null) {
                    return user;
                }
            } catch (NumberFormatException ignored) {
                // fall through to id-based lookup
            }
        }
        if (subscriptionId != null && !subscriptionId.isBlank()) {
            User user = userRepository.findByStripeSubscriptionId(subscriptionId).orElse(null);
            if (user != null) {
                return user;
            }
        }
        if (customerId != null && !customerId.isBlank()) {
            return userRepository.findByStripeCustomerId(customerId).orElse(null);
        }
        return null;
    }
}
