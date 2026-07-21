package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.chinesereads.backend.Security.jwt.JwtTokenProvider;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;

/**
 * Pins the signing-key contract that keeps sessions alive across redeploys: two
 * providers configured with the same jwt.secret must accept each other's tokens
 * (that is what "the same app after a restart" means), while providers without a
 * configured secret get independent random keys, and weak secrets abort startup.
 */
public class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-0123456789-0123456789-0123456789";

    private final UserDetails userDetails = new User("maria", "irrelevant",
            List.of(new SimpleGrantedAuthority("ROLE_USER")));

    @Test
    @DisplayName("The same configured secret validates tokens across provider instances (redeploy survival)")
    public void configuredSecretSurvivesRestart() {
        String token = new JwtTokenProvider(SECRET).generateAccessToken(userDetails);

        // A brand-new provider = the app after a redeploy. Same secret, same sessions.
        assertEquals("maria", new JwtTokenProvider(SECRET).validateToken(token).getSubject());
    }

    @Test
    @DisplayName("Without a configured secret each boot gets its own random key")
    public void blankSecretGeneratesPerBootKey() {
        JwtTokenProvider boot1 = new JwtTokenProvider("");
        String token = boot1.generateAccessToken(userDetails);

        // The issuing boot accepts its own token; a fresh boot does not.
        assertNotNull(boot1.validateToken(token));
        assertThrows(JwtException.class, () -> new JwtTokenProvider("").validateToken(token));
    }

    @Test
    @DisplayName("A secret under 32 bytes aborts startup instead of signing weakly")
    public void shortSecretIsRejected() {
        assertThrows(WeakKeyException.class, () -> new JwtTokenProvider("too-short"));
    }
}
