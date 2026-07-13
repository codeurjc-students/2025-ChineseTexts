package com.chinesereads.backend.Controller;

import java.security.Principal;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;

/**
 * PREMIUM subscription endpoints backed by Stripe.
 *
 * <ul>
 *   <li>{@code POST /api/premium/checkout} — start a subscription (auth): returns a
 *       Stripe-hosted Checkout URL to redirect to.</li>
 *   <li>{@code POST /api/premium/portal} — open the Stripe billing portal (auth) to
 *       manage or cancel the subscription.</li>
 *   <li>{@code POST /api/premium/webhook} — Stripe → us (public, signature-verified):
 *       applies subscription changes to the user's premium status.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/premium")
public class PremiumController {

    private static final Logger log = LoggerFactory.getLogger(PremiumController.class);

    private final StripeService stripeService;

    private final UserRepository userRepository;

    public PremiumController(StripeService stripeService, UserRepository userRepository) {
        this.stripeService = stripeService;
        this.userRepository = userRepository;
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody Map<String, String> body, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!stripeService.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("code", "BILLING_UNAVAILABLE", "message", "Billing is not available right now"));
        }
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String plan = body.getOrDefault("plan", "monthly");
        try {
            String url = stripeService.createCheckoutSession(user, plan);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "PLAN_UNAVAILABLE", "message", "That plan is not available"));
        } catch (StripeException e) {
            log.error("Stripe checkout failed", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("code", "CHECKOUT_FAILED", "message", "Could not start checkout"));
        }
    }

    @PostMapping("/portal")
    public ResponseEntity<?> portal(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!stripeService.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("code", "BILLING_UNAVAILABLE", "message", "Billing is not available right now"));
        }
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            String url = stripeService.createPortalSession(user);
            if (url == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("code", "NO_SUBSCRIPTION", "message", "No subscription to manage"));
            }
            return ResponseEntity.ok(Map.of("url", url));
        } catch (StripeException e) {
            log.error("Stripe portal failed", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("code", "PORTAL_FAILED", "message", "Could not open the billing portal"));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        try {
            stripeService.handleWebhook(payload, signature);
            return ResponseEntity.ok("");
        } catch (SignatureVerificationException e) {
            // Bad/spoofed signature — reject so Stripe records the failure.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (StripeException e) {
            log.error("Stripe webhook processing failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook error");
        } catch (Exception e) {
            // Never leak internals; still 500 so Stripe retries transient failures.
            log.error("Unexpected webhook error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook error");
        }
    }
}
