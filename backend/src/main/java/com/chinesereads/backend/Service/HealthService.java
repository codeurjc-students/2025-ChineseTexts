package com.chinesereads.backend.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.chinesereads.backend.Repository.TextRepository;

/**
 * Aggregated health check for the uptime monitor (GET /api/health). The Python
 * microservices are NOT exposed to the internet — this service probes them from
 * INSIDE the Docker network (their /health routes, which never call the paid
 * external APIs) and only the summary goes out. Short timeouts so a dead
 * service makes this endpoint answer "DOWN" quickly instead of hanging.
 */
@Service
public class HealthService {

    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(2);

    private final TextRepository textRepository;
    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:5001}")
    private String aiServiceUrl;

    @Value("${ocr.service.url:http://localhost:5000}")
    private String ocrServiceUrl;

    @Value("${tts.service.url:http://localhost:5002}")
    private String ttsServiceUrl;

    // Con dos constructores, Spring necesita saber cuál usar: este.
    @org.springframework.beans.factory.annotation.Autowired
    public HealthService(TextRepository textRepository) {
        this(textRepository, buildProbeRestTemplate());
    }

    // Visible for tests: lets the probe client be replaced by a mock.
    HealthService(TextRepository textRepository, RestTemplate restTemplate) {
        this.textRepository = textRepository;
        this.restTemplate = restTemplate;
    }

    private static RestTemplate buildProbeRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) PROBE_TIMEOUT.toMillis());
        factory.setReadTimeout((int) PROBE_TIMEOUT.toMillis());
        return new RestTemplate(factory);
    }

    /** Status of every dependency, in stable display order: UP or DOWN. */
    public Map<String, String> check() {
        Map<String, String> services = new LinkedHashMap<>();
        services.put("database", checkDatabase());
        services.put("ai-service", checkHttp(aiServiceUrl));
        services.put("ocr-service", checkHttp(ocrServiceUrl));
        services.put("tts-service", checkHttp(ttsServiceUrl));
        return services;
    }

    public boolean allUp(Map<String, String> services) {
        return services.values().stream().allMatch("UP"::equals);
    }

    /**
     * Probes a single dependency, for the per-service monitor endpoints
     * (GET /api/health/{service}). Returns null for unknown keys (-> 404).
     */
    public String checkOne(String service) {
        return switch (service) {
            case "database" -> checkDatabase();
            case "ai" -> checkHttp(aiServiceUrl);
            case "ocr" -> checkHttp(ocrServiceUrl);
            case "tts" -> checkHttp(ttsServiceUrl);
            default -> null;
        };
    }

    private String checkDatabase() {
        try {
            textRepository.count();
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String checkHttp(String baseUrl) {
        try {
            restTemplate.getForEntity(baseUrl + "/health", String.class);
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
