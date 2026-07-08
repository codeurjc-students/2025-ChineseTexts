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
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
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

        String customerId = user.getStripeCustomerId();
        if (customerId == null || customerId.isBlank()) {
            Customer customer = Customer.create(CustomerCreateParams.builder()
                    .setEmail(user.getEmail())
                    .setName(user.getName())
                    .build());
            customerId = customer.getId();
            user.setStripeCustomerId(customerId);
            userRepository.save(user);
        }

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
     * Creates a Stripe Billing Portal session so the user can manage or cancel their
     * subscription. Returns {@code null} if the user has no Stripe customer yet.
     */
    public String createPortalSession(User user) throws StripeException {
        ensureApiKey();
        if (user.getStripeCustomerId() == null || user.getStripeCustomerId().isBlank()) {
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
        StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);

        switch (event.getType()) {
            // First payment: grant premium and record the Stripe ids.
            case "checkout.session.completed" -> {
                if (object instanceof Session session) {
                    applySubscription(session.getClientReferenceId(), session.getCustomer(),
                            session.getSubscription(), periodEndOf(session.getSubscription()));
                }
            }
            // Renewals AND status changes deliver the Subscription object directly (its
            // period end advances on each successful renewal).
            case "customer.subscription.updated", "customer.subscription.created" -> {
                if (object instanceof Subscription subscription) {
                    String status = subscription.getStatus();
                    if ("active".equals(status) || "trialing".equals(status)) {
                        applySubscription(null, subscription.getCustomer(), subscription.getId(),
                                periodEndOf(subscription));
                    } else if ("canceled".equals(status) || "unpaid".equals(status)) {
                        revokeSubscription(subscription.getId());
                    }
                }
            }
            case "customer.subscription.deleted" -> {
                if (object instanceof Subscription subscription) {
                    revokeSubscription(subscription.getId());
                }
            }
            default -> log.debug("Ignoring Stripe event type {}", event.getType());
        }
    }

    /** Retrieves a subscription by id and reads its current period end, or null. */
    private Long periodEndOf(String subscriptionId) throws StripeException {
        if (subscriptionId == null || subscriptionId.isBlank()) {
            return null;
        }
        return periodEndOf(Subscription.retrieve(subscriptionId));
    }

    /**
     * Reads the current period end (epoch seconds) from a subscription. In the current
     * Stripe API this lives on the subscription item, not the subscription itself.
     */
    private Long periodEndOf(Subscription subscription) {
        if (subscription == null || subscription.getItems() == null
                || subscription.getItems().getData() == null
                || subscription.getItems().getData().isEmpty()) {
            return null;
        }
        return subscription.getItems().getData().get(0).getCurrentPeriodEnd();
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
