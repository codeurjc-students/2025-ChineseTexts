package com.chinesereads.backend.Controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chinesereads.backend.Service.HealthService;

/**
 * Public health summary for external uptime monitoring. Returns 200 when the
 * whole chain (backend + database + the three internal Python services) is
 * healthy and 503 otherwise, so a plain HTTP monitor alerts on any failure.
 * Deliberately minimal information: service names and UP/DOWN, nothing else.
 */
@CrossOrigin
@RestController
@RequestMapping("/api/health")
public class HealthControllerRest {

    private final HealthService healthService;

    public HealthControllerRest(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, String> services = healthService.check();
        boolean allUp = healthService.allUp(services);
        Map<String, Object> body = Map.of(
                "status", allUp ? "UP" : "DEGRADED",
                "services", services);
        return ResponseEntity.status(allUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(body);
    }
}
