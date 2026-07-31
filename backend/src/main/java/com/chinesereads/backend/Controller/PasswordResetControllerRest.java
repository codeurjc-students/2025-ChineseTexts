package com.chinesereads.backend.Controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chinesereads.backend.Service.PasswordResetRateLimiterService;
import com.chinesereads.backend.Service.PasswordResetService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Endpoints anónimos del flujo "olvidé mi contraseña". Ambos son públicos
 * (declarados explícitamente en SecurityConfig): el visitante no tiene sesión
 * y la autenticación real es el token de un solo uso que viaja por email.
 *
 * Como en el signup, cada error lleva un "code" estable que el frontend traduce
 * con Transloco (flujo anónimo: el backend no conoce el idioma del cliente).
 */
@CrossOrigin
@RestController
@RequestMapping("/api/auth")
public class PasswordResetControllerRest {

    private final PasswordResetService passwordResetService;
    private final PasswordResetRateLimiterService rateLimiter;

    public PasswordResetControllerRest(PasswordResetService passwordResetService,
                                       PasswordResetRateLimiterService rateLimiter) {
        this.passwordResetService = passwordResetService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Pide un enlace de restablecimiento para el email dado. Responde 200 con el
     * MISMO cuerpo exista o no la cuenta (anti-enumeración: la respuesta jamás
     * revela qué emails están registrados; el envío real es asíncrono, así que
     * tampoco lo delata el tiempo). Limitado por IP para no bombardear buzones
     * ajenos ni agotar la cuota de envío.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body,
                                            HttpServletRequest request) {
        if (!rateLimiter.tryAcquire(clientKey(request))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("code", "TOO_MANY_REQUESTS",
                            "message", "Too many reset requests, try again later"));
        }
        String email = body.get("email");
        if (email == null || !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            // Formato inválido: no hay nada que procesar y no revela existencia alguna.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "INVALID_EMAIL", "message", "A valid email is required"));
        }
        passwordResetService.requestReset(email);
        return ResponseEntity.ok(Map.of("message",
                "If an account exists for that email, a reset link has been sent"));
    }

    /**
     * Consume el token del correo y fija la nueva contraseña. Un único código de
     * error para token desconocido/caducado/ya usado — detallar el motivo daría
     * información gratis a un atacante probando tokens.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        // Mismo mínimo que el registro (UserControllerRest.registerUser); el
        // frontend aplica su propio mínimo de 6 como en el formulario de signup.
        if (newPassword == null || newPassword.length() < 4) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "PASSWORD_TOO_SHORT",
                            "message", "Password must be at least 4 characters"));
        }
        boolean done = passwordResetService.resetPassword(token, newPassword);
        if (!done) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "INVALID_OR_EXPIRED_TOKEN",
                            "message", "The reset link is invalid or has expired"));
        }
        return ResponseEntity.ok(Map.of("message", "Password updated"));
    }

    /**
     * Best-effort client identifier for rate limiting. The app runs behind the
     * Caddy reverse proxy, which forwards the real client IP in X-Forwarded-For;
     * fall back to the direct remote address when the header is absent.
     * (Mismo helper que TtsController — deliberadamente local a cada controlador.)
     */
    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For may be a comma-separated list; the first entry is the client.
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String remote = request.getRemoteAddr();
        return remote != null ? remote : "unknown";
    }
}
