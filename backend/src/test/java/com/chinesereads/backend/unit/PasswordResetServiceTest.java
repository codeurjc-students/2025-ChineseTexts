package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.EmailService;
import com.chinesereads.backend.Service.PasswordResetService;

/**
 * Pins the security invariants of the forgot-password flow: only the SHA-256 hash of
 * the token is ever persisted (never the raw token), unknown/blocked accounts cause
 * no action (anti-enumeration), the token is single-use with a bounded lifetime, and
 * consuming it clears it.
 */
@ExtendWith(MockitoExtension.class)
public class PasswordResetServiceTest {

    private static final int TOKEN_MINUTES = 60;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    private PasswordResetService service;

    private User user;

    @BeforeEach
    public void setUp() {
        service = new PasswordResetService(userRepository, passwordEncoder, emailService, TOKEN_MINUTES);
        user = new User();
        user.setEmail("ana@test.com");
        user.setName("Ana");
        user.setLanguage("es");
    }

    // Test unitario 1: solicitar reset guarda el HASH (no el token) y envía el token en claro por email
    @Test
    @DisplayName("requestReset stores only the SHA-256 hash and emails the raw token with its lifetime")
    public void testRequestResetStoresHashAndSendsEmail() {
        when(userRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        service.requestReset("ana@test.com");

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("ana@test.com"), eq("Ana"), eq("es"),
                tokenCaptor.capture(), eq(TOKEN_MINUTES));
        String rawToken = tokenCaptor.getValue();
        assertNotNull(rawToken);
        assertNotNull(user.getPasswordResetTokenHash());
        // Lo persistido es el hash del token enviado, nunca el token en claro.
        assertEquals(PasswordResetService.sha256(rawToken), user.getPasswordResetTokenHash());
        assertNotEquals(rawToken, user.getPasswordResetTokenHash());
        assertTrue(user.getPasswordResetExpiresAt().isAfter(LocalDateTime.now()));
        assertTrue(user.getPasswordResetExpiresAt()
                .isBefore(LocalDateTime.now().plusMinutes(TOKEN_MINUTES + 1)));
        verify(userRepository).save(user);
    }

    // Test unitario 2: email desconocido → silencio absoluto (anti-enumeración)
    @Test
    @DisplayName("requestReset for an unknown email saves nothing and sends nothing")
    public void testRequestResetUnknownEmailIsSilent() {
        when(userRepository.findByEmail("nadie@test.com")).thenReturn(Optional.empty());

        service.requestReset("nadie@test.com");

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString(),
                anyString(), anyInt());
    }

    // Test unitario 3: cuenta bloqueada → sin token ni correo
    @Test
    @DisplayName("requestReset for a blocked account saves nothing and sends nothing")
    public void testRequestResetBlockedAccountIsSilent() {
        user.setBlocked(true);
        when(userRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        service.requestReset("ana@test.com");

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString(),
                anyString(), anyInt());
    }

    // Test unitario 4: reset con token válido cambia la contraseña (codificada) y lo consume
    @Test
    @DisplayName("resetPassword with a valid token encodes the new password and clears the token")
    public void testResetPasswordConsumesToken() {
        user.setPasswordResetTokenHash(PasswordResetService.sha256("tok-valido"));
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(30));
        when(userRepository.findByPasswordResetTokenHash(PasswordResetService.sha256("tok-valido")))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.encode("nueva-clave")).thenReturn("$2a$encoded");

        assertTrue(service.resetPassword("tok-valido", "nueva-clave"));

        assertEquals("$2a$encoded", user.getPassword());   // nunca en claro
        assertNull(user.getPasswordResetTokenHash());       // un solo uso
        assertNull(user.getPasswordResetExpiresAt());
        verify(userRepository).save(user);
    }

    // Test unitario 5: token caducado → false y sin cambios
    @Test
    @DisplayName("resetPassword with an expired token fails and changes nothing")
    public void testResetPasswordExpiredToken() {
        user.setPasswordResetTokenHash(PasswordResetService.sha256("tok-caducado"));
        user.setPasswordResetExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByPasswordResetTokenHash(PasswordResetService.sha256("tok-caducado")))
                .thenReturn(Optional.of(user));

        assertFalse(service.resetPassword("tok-caducado", "nueva-clave"));

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    // Test unitario 6: token desconocido o vacío → false
    @Test
    @DisplayName("resetPassword with an unknown or blank token fails")
    public void testResetPasswordUnknownToken() {
        when(userRepository.findByPasswordResetTokenHash(anyString())).thenReturn(Optional.empty());

        assertFalse(service.resetPassword("tok-desconocido", "nueva-clave"));
        assertFalse(service.resetPassword("", "nueva-clave"));
        assertFalse(service.resetPassword(null, "nueva-clave"));

        verify(userRepository, never()).save(any());
    }
}
