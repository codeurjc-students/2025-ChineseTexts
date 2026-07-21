package com.chinesereads.backend.Controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chinesereads.backend.Service.ActivationMetricsService;
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
 *   <li>{@code GET /api/influencers/settlements?from=&to=} — the commission settlement
 *       for a date range, computed from the local payment ledger (works even with
 *       Stripe down, so past settlements are always reachable).</li>
 *   <li>{@code GET /api/influencers/metrics?from=&to=} — activation metrics of the
 *       signup cohort of a date range (day-1 activation, D7 retention, premium),
 *       computed entirely from the local database.</li>
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
    private final ActivationMetricsService activationMetricsService;

    public InfluencerController(InfluencerService influencerService, StripeService stripeService,
            ActivationMetricsService activationMetricsService) {
        this.influencerService = influencerService;
        this.stripeService = stripeService;
        this.activationMetricsService = activationMetricsService;
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

    @GetMapping("/settlements")
    public ResponseEntity<?> settlements(@RequestParam String from, @RequestParam String to) {
        LocalDate fromDate;
        LocalDate toDate;
        try {
            fromDate = LocalDate.parse(from);
            toDate = LocalDate.parse(to);
        } catch (DateTimeParseException e) {
            return invalidRange();
        }
        if (fromDate.isAfter(toDate)) {
            return invalidRange();
        }
        return ResponseEntity.ok(influencerService.settlements(fromDate, toDate));
    }

    /** No Stripe involved: pure local-database cohort metrics, always available. */
    @GetMapping("/metrics")
    public ResponseEntity<?> metrics(@RequestParam String from, @RequestParam String to) {
        LocalDate fromDate;
        LocalDate toDate;
        try {
            fromDate = LocalDate.parse(from);
            toDate = LocalDate.parse(to);
        } catch (DateTimeParseException e) {
            return invalidRange();
        }
        if (fromDate.isAfter(toDate)) {
            return invalidRange();
        }
        return ResponseEntity.ok(activationMetricsService.metrics(fromDate, toDate, LocalDate.now()));
    }

    private ResponseEntity<?> invalidRange() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", "INVALID_RANGE", "message", "Invalid settlement date range"));
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
