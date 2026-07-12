package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.chinesereads.backend.Service.EmailService;

/**
 * Tests the email gate (disabled until configured, like Stripe) and the welcome
 * template builder (branding, language, links) without any real HTTP call.
 */
public class EmailServiceTest {

    private EmailService emailService;

    @BeforeEach
    public void setUp() {
        emailService = new EmailService();
        injectField(emailService, "publicUrl", "https://chinesereads.com");
        injectField(emailService, "fromName", "ChineseReads");
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Not configured until both API key and sender are present")
    public void testIsConfigured() {
        assertFalse(emailService.isConfigured());
        injectField(emailService, "apiKey", "xkeysib-test");
        assertFalse(emailService.isConfigured()); // sender still missing
        injectField(emailService, "fromEmail", "hello@chinesereads.com");
        assertTrue(emailService.isConfigured());
    }

    @Test
    @DisplayName("Sending while unconfigured is a silent no-op (never throws)")
    public void testUnconfiguredSendIsNoop() {
        assertDoesNotThrow(() -> emailService.sendWelcomeEmail("a@a.com", "A", "en"));
    }

    @Test
    @DisplayName("The English welcome is branded, personalized and links to the app")
    public void testWelcomeHtmlEnglish() {
        String html = emailService.buildWelcomeHtml("María", false);
        assertTrue(html.contains("https://chinesereads.com/icon-512.png")); // logo
        assertTrue(html.contains("Hi María!"));
        assertTrue(html.contains("https://chinesereads.com/texts"));        // CTA
        assertTrue(html.contains("https://chinesereads.com/level-test"));
        assertTrue(html.contains("Privacy policy"));
    }

    @Test
    @DisplayName("The Spanish welcome uses Spanish copy and /es links")
    public void testWelcomeHtmlSpanish() {
        String html = emailService.buildWelcomeHtml("María", true);
        assertTrue(html.contains("¡Hola, María!"));
        assertTrue(html.contains("https://chinesereads.com/es/texts"));
        assertTrue(html.contains("https://chinesereads.com/es/level-test"));
        assertTrue(html.contains("Política de privacidad"));
    }

    @Test
    @DisplayName("A null name never breaks the template")
    public void testNullNameSafe() {
        assertDoesNotThrow(() -> emailService.buildWelcomeHtml(null, true));
    }
}
