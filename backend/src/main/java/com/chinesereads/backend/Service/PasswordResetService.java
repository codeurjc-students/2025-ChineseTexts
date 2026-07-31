package com.chinesereads.backend.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;

/**
 * Autoservicio de "olvidé mi contraseña" (flujo anónimo, sin admin):
 *
 * <ol>
 *   <li>{@link #requestReset}: el visitante da su email; si existe una cuenta (y no
 *       está bloqueada) se le envía un enlace con un token aleatorio de un solo uso.
 *       La respuesta del endpoint es SIEMPRE la misma exista o no la cuenta, para no
 *       revelar qué emails están registrados (anti-enumeración).</li>
 *   <li>{@link #resetPassword}: el enlace del correo abre /reset-password?token=…;
 *       con token válido y no caducado se fija la nueva contraseña y el token se
 *       invalida (un solo uso).</li>
 * </ol>
 *
 * Seguridad del token: 32 bytes de {@link SecureRandom} en Base64-URL. En la base de
 * datos solo se guarda su hash SHA-256 ({@code User.passwordResetTokenHash}) — como
 * las contraseñas, nunca en claro — así un volcado de la BD no permite restablecer
 * cuentas ajenas. La caducidad es corta y configurable (60 min por defecto). Una
 * nueva solicitud sobreescribe la anterior (solo hay un token vivo por cuenta).
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final int tokenMinutes;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                EmailService emailService,
                                @Value("${password-reset.token-minutes:60}") int tokenMinutes) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.tokenMinutes = tokenMinutes;
    }

    /**
     * Genera y envía por email un enlace de restablecimiento si el email pertenece a
     * una cuenta no bloqueada. Silencioso en cualquier otro caso (anti-enumeración:
     * el controlador responde 200 idéntico siempre). El envío del correo es asíncrono
     * y a prueba de fallos (EmailService), así el tiempo de respuesta tampoco delata
     * si la cuenta existe.
     */
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        Optional<User> found = userRepository.findByEmail(email.trim());
        if (found.isEmpty()) {
            log.info("Password reset requested for unknown email (no action)");
            return;
        }
        User user = found.get();
        if (user.isBlocked()) {
            log.info("Password reset requested for blocked account {} (no action)", user.getId());
            return;
        }
        String token = generateToken();
        user.setPasswordResetTokenHash(sha256(token));
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(tokenMinutes));
        userRepository.save(user);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), user.getLanguage(),
                token, tokenMinutes);
        log.info("Password reset email queued for user {}", user.getId());
    }

    /**
     * Consume el token y fija la nueva contraseña (ya validada por el controlador).
     * Devuelve false con token desconocido, caducado o de cuenta bloqueada — el
     * controlador lo traduce a un único código INVALID_OR_EXPIRED_TOKEN sin detallar
     * el motivo (no dar pistas). Con éxito limpia el token: un solo uso.
     */
    public boolean resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Optional<User> found = userRepository.findByPasswordResetTokenHash(sha256(token));
        if (found.isEmpty()) {
            return false;
        }
        User user = found.get();
        if (user.isBlocked()
                || user.getPasswordResetExpiresAt() == null
                || user.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);
        log.info("Password reset completed for user {}", user.getId());
        return true;
    }

    /** 32 bytes de SecureRandom en Base64-URL sin relleno: apto para query param. */
    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 en hexadecimal — lo único que se persiste del token. Público: los
     *  tests (y cualquier futuro flujo de token) fijan así el mismo hasheado. */
    public static String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 es obligatorio en toda JVM; si faltase, mejor fallar que degradar.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
