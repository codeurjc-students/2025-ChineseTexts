package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.chinesereads.backend.Repository.TextRepository;
import com.chinesereads.backend.Service.HealthService;

public class HealthServiceTest {

    private TextRepository textRepository;
    private RestTemplate restTemplate;
    private HealthService healthService;

    @BeforeEach
    public void setUp() throws Exception {
        textRepository = mock(TextRepository.class);
        restTemplate = mock(RestTemplate.class);
        // El constructor de tests inyecta el RestTemplate mockeado
        var ctor = HealthService.class.getDeclaredConstructor(TextRepository.class, RestTemplate.class);
        ctor.setAccessible(true);
        healthService = ctor.newInstance(textRepository, restTemplate);
    }

    // Test unitario 1: con base de datos y los tres servicios respondiendo,
    // todo es UP y allUp() da verde
    @Test
    @DisplayName("All dependencies responding -> everything UP")
    public void testAllUp() {
        when(textRepository.count()).thenReturn(5L);
        when(restTemplate.getForEntity(contains("/health"), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"status\":\"ok\"}"));

        Map<String, String> services = healthService.check();

        assertEquals(4, services.size());
        assertTrue(services.values().stream().allMatch("UP"::equals));
        assertTrue(healthService.allUp(services));
    }

    // Test unitario 2: un microservicio caído se reporta DOWN sin tumbar el
    // resto del chequeo, y allUp() da rojo (el controlador devolverá 503)
    @Test
    @DisplayName("One dead microservice -> DOWN for it, degraded overall")
    public void testOneServiceDown() {
        when(textRepository.count()).thenReturn(5L);
        when(restTemplate.getForEntity(contains("/health"), eq(String.class)))
                .thenThrow(new ResourceAccessException("connection refused"));

        Map<String, String> services = healthService.check();

        assertEquals("UP", services.get("database"));
        assertEquals("DOWN", services.get("ai-service"));
        assertEquals("DOWN", services.get("ocr-service"));
        assertEquals("DOWN", services.get("tts-service"));
        assertFalse(healthService.allUp(services));
    }

    // Test unitario 3: la base de datos caída también se reporta DOWN
    @Test
    @DisplayName("Database failure -> database DOWN")
    public void testDatabaseDown() {
        when(textRepository.count()).thenThrow(new RuntimeException("db down"));
        when(restTemplate.getForEntity(contains("/health"), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        Map<String, String> services = healthService.check();

        assertEquals("DOWN", services.get("database"));
        assertFalse(healthService.allUp(services));
    }
}
