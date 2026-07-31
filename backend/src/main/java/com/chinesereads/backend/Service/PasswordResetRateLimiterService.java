package com.chinesereads.backend.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Limitador por IP (ventana fija de 1 hora) para POST /api/auth/forgot-password.
 *
 * El endpoint es anónimo y cada petición válida envía un correo, así que sin tope
 * una sola IP podría (a) bombardear el buzón de una víctima y (b) agotar la cuota
 * de envío de Brevo. Mismo patrón Caffeine que {@link TtsRateLimiterService} (esa
 * clase se deja intacta a propósito: su ventana de 1 minuto está cableada a su
 * caso de uso); las entradas caducan solas, la memoria queda acotada.
 */
@Service
public class PasswordResetRateLimiterService {

    private final int maxPerHour;
    private final Cache<String, AtomicInteger> counters;

    public PasswordResetRateLimiterService(
            @Value("${password-reset.rate-limit.per-hour:5}") int maxPerHour) {
        this.maxPerHour = maxPerHour;
        this.counters = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(1))
                .maximumSize(10_000)
                .build();
    }

    /**
     * Registra una petición del cliente dado e indica si está permitida.
     * Devuelve false una vez superado el máximo por hora para esa clave.
     */
    public boolean tryAcquire(String clientKey) {
        AtomicInteger counter = counters.get(clientKey, k -> new AtomicInteger(0));
        return counter.incrementAndGet() <= maxPerHour;
    }
}
