package com.chinesereads.backend.Controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chinesereads.backend.Service.InfluencerService;
import com.chinesereads.backend.Service.StripeService;
import com.chinesereads.backend.dto.InfluencerCodeDTO;
import com.chinesereads.backend.dto.InfluencerCreateDTO;
import com.stripe.exception.StripeException;

/**
 * ADMIN-only influencer campaign management (secured in SecurityConfig):
 *
 * <ul>
 *   <li>{@code GET /api/influencers} — the tracking table (codes + signups /
 *       conversions / active premium per influencer).</li>
 *   <li>{@code POST /api/influencers} — create a discount code in Stripe.</li>
 *   <li>{@code DELETE /api/influencers/{id}} — deactivate a code (Stripe keeps its
 *       redemption history; the code can never be redeemed again).</li>
 * </ul>
 *
 * Mirrors PremiumController's error contract: stable machine-readable codes, 503 when
 * billing is not configured, 502 when Stripe itself fails.
 */
@RestController
@RequestMapping("/api/influencers")
public class InfluencerController {

    private static final Logger log = LoggerFactory.getLogger(InfluencerController.class);

    private final InfluencerService influencerService;
    private final StripeService stripeService;

    public InfluencerController(InfluencerService influencerService, StripeService stripeService) {
        this.influencerService = influencerService;
        this.stripeService = stripeService;
    }

    @GetMapping("")
    public ResponseEntity<?> list() {
        if (!stripeService.isConfigured()) {
            return billingUnavailable();
        }
        try {
            List<InfluencerCodeDTO> stats = influencerService.getStats();
            return ResponseEntity.ok(stats);
        } catch (StripeException e) {
            log.error("Stripe promotion code listing failed", e);
            return stripeFailed();
        }
    }

    @PostMapping("")
    public ResponseEntity<?> create(@RequestBody InfluencerCreateDTO request) {
        if (!stripeService.isConfigured()) {
            return billingUnavailable();
        }
        try {
            InfluencerCodeDTO created = influencerService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            // The message IS the stable code (INVALID_CODE, INVALID_PERCENT, ...).
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", e.getMessage(), "message", "Invalid promotion code request"));
        } catch (StripeException e) {
            log.error("Stripe promotion code creation failed", e);
            return stripeFailed();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivate(@PathVariable String id) {
        if (!stripeService.isConfigured()) {
            return billingUnavailable();
        }
        try {
            influencerService.deactivate(id);
            return ResponseEntity.noContent().build();
        } catch (StripeException e) {
            log.error("Stripe promotion code deactivation failed", e);
            return stripeFailed();
        }
    }

    private ResponseEntity<?> billingUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("code", "BILLING_UNAVAILABLE", "message", "Billing is not available right now"));
    }

    private ResponseEntity<?> stripeFailed() {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("code", "STRIPE_FAILED", "message", "Stripe request failed"));
    }
}
