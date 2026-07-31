package com.chinesereads.backend.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Transactional email via the Brevo API (https://api.brevo.com/v3/smtp/email).
 *
 * <p>Follows the same convention as the Stripe integration: all credentials come from
 * the environment and are empty by default, so the app boots and runs normally with
 * email disabled ({@link #isConfigured()} gates every send). Sends are asynchronous and
 * failures are swallowed (logged) — an email problem must never break a signup.
 *
 * <p>Legal note: the welcome email is TRANSACTIONAL (service communication tied to the
 * account the user just created), which does not require marketing consent. Marketing
 * emails (tips, digests) are a separate, future concern gated by {@code User.emailConsent}.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    @Value("${brevo.api-key:}")
    private String apiKey;

    @Value("${mail.from:}")
    private String fromEmail;

    @Value("${mail.from-name:ChineseReads}")
    private String fromName;

    @Value("${app.public-url:https://chinesereads.com}")
    private String publicUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /** True only when a Brevo API key and sender are present. */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && fromEmail != null && !fromEmail.isBlank();
    }

    /**
     * Sends the welcome email (in the user's language) after a successful signup.
     * Fire-and-forget: runs on a background thread and never throws to the caller.
     */
    public void sendWelcomeEmail(String toEmail, String toName, String language) {
        if (!isConfigured()) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                boolean es = "es".equalsIgnoreCase(language);
                String subject = es
                        ? "¡Bienvenido a ChineseReads! Tu primera lectura te espera 🇨🇳"
                        : "Welcome to ChineseReads! Your first reading is waiting 🇨🇳";
                send(toEmail, toName, subject, buildWelcomeHtml(toName, es));
                log.info("Welcome email sent to {}", toEmail);
            } catch (Exception e) {
                log.warn("Could not send welcome email to {} (signup unaffected)", toEmail, e);
            }
        });
    }

    /**
     * Sends the daily SRS review reminder (marketing: only for users who opted in via
     * {@code emailConsent}). SYNCHRONOUS on purpose — the caller is the scheduled
     * reminder job, which already runs on a background thread and needs the failure
     * to propagate so an unsent reminder is not marked as sent (it retries next day).
     */
    public void sendReviewReminderEmail(String toEmail, String toName, String language,
                                        long dueCount, int streak, String unsubscribeToken) {
        if (!isConfigured()) {
            return;
        }
        boolean es = "es".equalsIgnoreCase(language);
        String subject = es
                ? "Tienes " + dueCount + (dueCount == 1 ? " tarjeta esperándote" : " tarjetas esperándote") + " hoy 📚"
                : "You have " + dueCount + (dueCount == 1 ? " flashcard waiting" : " flashcards waiting") + " today 📚";
        String unsubscribeUrl = publicUrl + "/api/users/unsubscribe?token=" + unsubscribeToken;
        // List-Unsubscribe: standard header mail clients (Gmail, Apple Mail) surface as
        // a native "Unsubscribe" button; also improves deliverability of marketing mail.
        send(toEmail, toName, subject,
                buildReviewReminderHtml(toName, es, dueCount, streak, unsubscribeToken),
                Map.of("List-Unsubscribe", "<" + unsubscribeUrl + ">"));
        log.info("Review reminder ({} due) sent to {}", dueCount, toEmail);
    }

    /**
     * Sends the password-reset link (in the user's language). TRANSACTIONAL (the user
     * just asked for it), so it does not depend on {@code emailConsent} and carries no
     * unsubscribe link. Fire-and-forget like the welcome email — and deliberately so:
     * the forgot-password endpoint answers the same, in the same time, whether the
     * account exists or not, so the send must never delay or break the response.
     */
    public void sendPasswordResetEmail(String toEmail, String toName, String language,
                                       String token, int expiryMinutes) {
        if (!isConfigured()) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                boolean es = "es".equalsIgnoreCase(language);
                String subject = es
                        ? "Restablece tu contraseña de ChineseReads 🔑"
                        : "Reset your ChineseReads password 🔑";
                send(toEmail, toName, subject, buildPasswordResetHtml(toName, es, token, expiryMinutes));
                log.info("Password reset email sent to {}", toEmail);
            } catch (Exception e) {
                log.warn("Could not send password reset email to {}", toEmail, e);
            }
        });
    }

    /** Posts one transactional email to Brevo. */
    private void send(String toEmail, String toName, String subject, String html) {
        send(toEmail, toName, subject, html, Map.of());
    }

    /** Posts one email to Brevo, optionally with extra SMTP headers (e.g. List-Unsubscribe). */
    private void send(String toEmail, String toName, String subject, String html,
                      Map<String, String> extraHeaders) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);

        Map<String, Object> body = new java.util.HashMap<>(Map.of(
                "sender", Map.of("email", fromEmail, "name", fromName),
                "to", java.util.List.of(Map.of("email", toEmail, "name", toName)),
                "subject", subject,
                "htmlContent", html));
        if (!extraHeaders.isEmpty()) {
            body.put("headers", extraHeaders);
        }

        restTemplate.postForEntity(BREVO_URL, new HttpEntity<>(body, headers), String.class);
    }

    /**
     * The welcome email body: a self-contained, corporate-looking HTML built with
     * table layout and inline styles (the only reliable way across email clients),
     * branded with the ChineseReads logo and colors. Exposed for testing.
     */
    public String buildWelcomeHtml(String name, boolean es) {
        String base = publicUrl;
        String lp = es ? base + "/es" : base;          // language-prefixed links
        String logo = base + "/icon-512.png";
        String safeName = name == null ? "" : name.trim();

        String hello = es ? "¡Hola, " + safeName + "! 你好!" : "Hi " + safeName + "! 你好!";
        String intro = es
                ? "Tu cuenta está lista. ChineseReads te ayuda a aprender chino leyendo textos graduados por nivel HSK, con pinyin, traducciones, audio y un tutor IA."
                : "Your account is ready. ChineseReads helps you learn Chinese by reading HSK-graded texts with pinyin, translations, audio and an AI tutor.";
        String f1t = es ? "Descubre tu nivel" : "Find your level";
        String f1d = es ? "Haz el test gratuito de 3 minutos y empieza justo donde debes." : "Take the free 3-minute test and start exactly where you should.";
        String f2t = es ? "Lee tu primera historia" : "Read your first story";
        String f2d = es ? "Toca cualquier palabra para ver su pinyin y traducción al instante." : "Tap any word to see its pinyin and translation instantly.";
        String f3t = es ? "Construye tu racha" : "Build your streak";
        String f3d = es ? "Una lectura al día enciende tu racha 🔥 y crea el hábito." : "One reading a day lights your streak 🔥 and builds the habit.";
        String cta = es ? "Empieza a leer" : "Start reading";
        String ctaTest = es ? "O haz primero el test de nivel →" : "Or take the level test first →";
        String footer = es
                ? "Recibes este correo porque acabas de crear una cuenta en ChineseReads."
                : "You're receiving this email because you just created a ChineseReads account.";
        String privacy = es ? "Política de privacidad" : "Privacy policy";

        return """
<!doctype html>
<html>
<body style="margin:0;padding:0;background-color:#f4f1ec;">
  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f1ec;padding:24px 0;">
    <tr><td align="center">
      <table role="presentation" width="600" cellpadding="0" cellspacing="0"
             style="width:600px;max-width:94%%;background:#ffffff;border-radius:12px;overflow:hidden;font-family:Arial,Helvetica,sans-serif;">
        <tr>
          <td style="background:#c0392b;padding:26px 32px;" align="center">
            <img src="%LOGO%" width="56" height="56" alt="ChineseReads"
                 style="display:block;border-radius:12px;margin:0 auto 10px;">
            <div style="color:#ffffff;font-size:22px;font-weight:bold;letter-spacing:.3px;">ChineseReads</div>
          </td>
        </tr>
        <tr>
          <td style="padding:32px 36px 8px;">
            <h1 style="margin:0 0 12px;font-size:22px;color:#2c3e50;">%HELLO%</h1>
            <p style="margin:0;font-size:15px;line-height:1.6;color:#444;">%INTRO%</p>
          </td>
        </tr>
        <tr>
          <td style="padding:20px 36px 4px;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
              <tr><td style="padding:10px 0;border-bottom:1px solid #f0ece6;">
                <div style="font-size:15px;color:#c0392b;font-weight:bold;">1 · %F1T%</div>
                <div style="font-size:14px;color:#555;line-height:1.5;">%F1D%</div>
              </td></tr>
              <tr><td style="padding:10px 0;border-bottom:1px solid #f0ece6;">
                <div style="font-size:15px;color:#c0392b;font-weight:bold;">2 · %F2T%</div>
                <div style="font-size:14px;color:#555;line-height:1.5;">%F2D%</div>
              </td></tr>
              <tr><td style="padding:10px 0;">
                <div style="font-size:15px;color:#c0392b;font-weight:bold;">3 · %F3T%</div>
                <div style="font-size:14px;color:#555;line-height:1.5;">%F3D%</div>
              </td></tr>
            </table>
          </td>
        </tr>
        <tr>
          <td align="center" style="padding:22px 36px 8px;">
            <a href="%LP%/texts"
               style="display:inline-block;background:#27ae60;color:#ffffff;text-decoration:none;
                      font-size:16px;font-weight:bold;padding:13px 34px;border-radius:8px;">%CTA%</a>
          </td>
        </tr>
        <tr>
          <td align="center" style="padding:4px 36px 26px;">
            <a href="%LP%/level-test" style="font-size:13px;color:#c0392b;text-decoration:none;">%CTATEST%</a>
          </td>
        </tr>
        <tr>
          <td style="background:#faf9f7;padding:18px 36px;border-top:1px solid #eee;" align="center">
            <p style="margin:0 0 6px;font-size:12px;color:#999;line-height:1.5;">%FOOTER%</p>
            <a href="%LP%/privacy-policy" style="font-size:12px;color:#999;">%PRIVACY%</a>
          </td>
        </tr>
      </table>
    </td></tr>
  </table>
</body>
</html>
"""
                .replace("%LOGO%", logo)
                .replace("%HELLO%", hello)
                .replace("%INTRO%", intro)
                .replace("%F1T%", f1t).replace("%F1D%", f1d)
                .replace("%F2T%", f2t).replace("%F2D%", f2d)
                .replace("%F3T%", f3t).replace("%F3D%", f3d)
                .replace("%CTA%", cta)
                .replace("%CTATEST%", ctaTest)
                .replace("%FOOTER%", footer)
                .replace("%PRIVACY%", privacy)
                .replace("%LP%", lp);
    }

    /**
     * The password-reset body: same corporate table/inline-style skeleton as the other
     * emails. Deliberately minimal — one explanation, one CTA with the tokenized link,
     * the expiry window, and the standard "ignore this if it wasn't you" note (the
     * account stays untouched until the link is used). The link carries the user's
     * language prefix so the reset page opens in their language. Exposed for testing.
     */
    public String buildPasswordResetHtml(String name, boolean es, String token, int expiryMinutes) {
        String base = publicUrl;
        String lp = es ? base + "/es" : base;
        String logo = base + "/icon-512.png";
        String safeName = name == null ? "" : name.trim();
        String resetUrl = lp + "/reset-password?token=" + token;

        String hello = es ? "¡Hola, " + safeName + "! 你好!" : "Hi " + safeName + "! 你好!";
        String intro = es
                ? "Hemos recibido una solicitud para restablecer la contraseña de tu cuenta de ChineseReads. Pulsa el botón para elegir una nueva:"
                : "We received a request to reset the password of your ChineseReads account. Click the button to choose a new one:";
        String cta = es ? "Restablecer contraseña" : "Reset password";
        String expiry = es
                ? "El enlace caduca en " + expiryMinutes + " minutos y solo puede usarse una vez."
                : "The link expires in " + expiryMinutes + " minutes and can only be used once.";
        String notYou = es
                ? "¿No has sido tú? Ignora este correo: tu contraseña seguirá siendo la misma y nadie puede cambiarla sin este enlace."
                : "Didn't request this? Just ignore this email: your password stays the same and nobody can change it without this link.";
        String footer = es
                ? "Recibes este correo porque alguien solicitó restablecer la contraseña de esta cuenta en ChineseReads."
                : "You're receiving this email because someone requested a password reset for this ChineseReads account.";
        String privacy = es ? "Política de privacidad" : "Privacy policy";

        return """
<!doctype html>
<html>
<body style="margin:0;padding:0;background-color:#f4f1ec;">
  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f1ec;padding:24px 0;">
    <tr><td align="center">
      <table role="presentation" width="600" cellpadding="0" cellspacing="0"
             style="width:600px;max-width:94%%;background:#ffffff;border-radius:12px;overflow:hidden;font-family:Arial,Helvetica,sans-serif;">
        <tr>
          <td style="background:#c0392b;padding:26px 32px;" align="center">
            <img src="%LOGO%" width="56" height="56" alt="ChineseReads"
                 style="display:block;border-radius:12px;margin:0 auto 10px;">
            <div style="color:#ffffff;font-size:22px;font-weight:bold;letter-spacing:.3px;">ChineseReads</div>
          </td>
        </tr>
        <tr>
          <td style="padding:32px 36px 8px;">
            <h1 style="margin:0 0 12px;font-size:22px;color:#2c3e50;">%HELLO%</h1>
            <p style="margin:0;font-size:15px;line-height:1.6;color:#444;">%INTRO%</p>
          </td>
        </tr>
        <tr>
          <td align="center" style="padding:24px 36px 10px;">
            <a href="%RESETURL%"
               style="display:inline-block;background:#27ae60;color:#ffffff;text-decoration:none;
                      font-size:16px;font-weight:bold;padding:13px 34px;border-radius:8px;">%CTA%</a>
          </td>
        </tr>
        <tr>
          <td align="center" style="padding:0 36px 6px;">
            <p style="margin:0;font-size:13px;color:#777;line-height:1.6;">%EXPIRY%</p>
          </td>
        </tr>
        <tr>
          <td style="padding:14px 36px 26px;">
            <p style="margin:0;font-size:13px;line-height:1.6;color:#999;">%NOTYOU%</p>
          </td>
        </tr>
        <tr>
          <td style="background:#faf9f7;padding:18px 36px;border-top:1px solid #eee;" align="center">
            <p style="margin:0 0 6px;font-size:12px;color:#999;line-height:1.5;">%FOOTER%</p>
            <a href="%LP%/privacy-policy" style="font-size:12px;color:#999;">%PRIVACY%</a>
          </td>
        </tr>
      </table>
    </td></tr>
  </table>
</body>
</html>
"""
                .replace("%LOGO%", logo)
                .replace("%HELLO%", hello)
                .replace("%INTRO%", intro)
                .replace("%RESETURL%", resetUrl)
                .replace("%CTA%", cta)
                .replace("%EXPIRY%", expiry)
                .replace("%NOTYOU%", notYou)
                .replace("%FOOTER%", footer)
                .replace("%PRIVACY%", privacy)
                .replace("%LP%", lp);
    }

    /**
     * The daily review-reminder body: same corporate table/inline-style skeleton as the
     * welcome email. Short by design — one number, one optional streak nudge, one CTA.
     * The footer explains WHY the user receives it (opted-in reminders) and offers a
     * one-click, no-login unsubscribe (GDPR: withdrawing must be as easy as giving).
     * Exposed for testing.
     */
    public String buildReviewReminderHtml(String name, boolean es, long dueCount, int streak,
                                          String unsubscribeToken) {
        String base = publicUrl;
        String lp = es ? base + "/es" : base;
        String logo = base + "/icon-512.png";
        String safeName = name == null ? "" : name.trim();
        String unsubscribeUrl = base + "/api/users/unsubscribe?token=" + unsubscribeToken;

        String hello = es ? "¡Hola, " + safeName + "! 你好!" : "Hi " + safeName + "! 你好!";
        String intro = es
                ? "Tus repasos de hoy están listos: <strong>" + dueCount
                  + (dueCount == 1 ? " tarjeta espera" : " tarjetas esperan")
                  + "</strong> en tus colecciones. Son solo unos minutos, y es justo el momento en que repasarlas fija las palabras en tu memoria."
                : "Today's reviews are ready: <strong>" + dueCount
                  + (dueCount == 1 ? " flashcard is waiting" : " flashcards are waiting")
                  + "</strong> in your collections. It only takes a few minutes, and right now is when reviewing locks those words into your memory.";
        String streakLine = streak <= 0 ? ""
                : (es
                    ? "🔥 Llevas <strong>" + streak + (streak == 1 ? " día" : " días")
                      + "</strong> de racha leyendo — una lectura hoy la mantiene viva."
                    : "🔥 You're on a <strong>" + streak + (streak == 1 ? "-day" : "-day")
                      + "</strong> reading streak — one reading today keeps it alive.");
        String cta = es ? "Repasar ahora" : "Review now";
        String footer = es
                ? "Recibes este correo porque activaste las comunicaciones por email en tu cuenta de ChineseReads. Como máximo te enviaremos un recordatorio al día, y solo cuando tengas repasos pendientes."
                : "You're receiving this email because you enabled email updates in your ChineseReads account. We send at most one reminder a day, and only when you have reviews due.";
        String unsubscribe = es ? "Darse de baja con un clic" : "Unsubscribe with one click";
        String privacy = es ? "Política de privacidad" : "Privacy policy";

        return """
<!doctype html>
<html>
<body style="margin:0;padding:0;background-color:#f4f1ec;">
  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f1ec;padding:24px 0;">
    <tr><td align="center">
      <table role="presentation" width="600" cellpadding="0" cellspacing="0"
             style="width:600px;max-width:94%%;background:#ffffff;border-radius:12px;overflow:hidden;font-family:Arial,Helvetica,sans-serif;">
        <tr>
          <td style="background:#c0392b;padding:26px 32px;" align="center">
            <img src="%LOGO%" width="56" height="56" alt="ChineseReads"
                 style="display:block;border-radius:12px;margin:0 auto 10px;">
            <div style="color:#ffffff;font-size:22px;font-weight:bold;letter-spacing:.3px;">ChineseReads</div>
          </td>
        </tr>
        <tr>
          <td style="padding:32px 36px 8px;">
            <h1 style="margin:0 0 12px;font-size:22px;color:#2c3e50;">%HELLO%</h1>
            <p style="margin:0;font-size:15px;line-height:1.6;color:#444;">%INTRO%</p>
          </td>
        </tr>
        %STREAKROW%
        <tr>
          <td align="center" style="padding:24px 36px 30px;">
            <a href="%LP%/collections"
               style="display:inline-block;background:#27ae60;color:#ffffff;text-decoration:none;
                      font-size:16px;font-weight:bold;padding:13px 34px;border-radius:8px;">%CTA%</a>
          </td>
        </tr>
        <tr>
          <td style="background:#faf9f7;padding:18px 36px;border-top:1px solid #eee;" align="center">
            <p style="margin:0 0 6px;font-size:12px;color:#999;line-height:1.5;">%FOOTER%</p>
            <a href="%UNSUB%" style="font-size:12px;color:#999;">%UNSUBTEXT%</a>
            &nbsp;·&nbsp;
            <a href="%LP%/privacy-policy" style="font-size:12px;color:#999;">%PRIVACY%</a>
          </td>
        </tr>
      </table>
    </td></tr>
  </table>
</body>
</html>
"""
                .replace("%STREAKROW%", streakLine.isEmpty() ? "" : """
        <tr>
          <td style="padding:14px 36px 0;">
            <p style="margin:0;font-size:14px;line-height:1.6;color:#555;">%STREAK%</p>
          </td>
        </tr>
""".replace("%STREAK%", streakLine))
                .replace("%LOGO%", logo)
                .replace("%HELLO%", hello)
                .replace("%INTRO%", intro)
                .replace("%CTA%", cta)
                .replace("%FOOTER%", footer)
                .replace("%UNSUB%", unsubscribeUrl)
                .replace("%UNSUBTEXT%", unsubscribe)
                .replace("%PRIVACY%", privacy)
                .replace("%LP%", lp);
    }
}
