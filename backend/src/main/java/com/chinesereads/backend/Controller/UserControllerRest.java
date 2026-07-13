package com.chinesereads.backend.Controller;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.chinesereads.backend.Service.UserService;
import com.chinesereads.backend.dto.AdminUserDTO;
import com.chinesereads.backend.dto.AdminUserUpdateDTO;
import com.chinesereads.backend.dto.UserDTO;

import jakarta.servlet.http.HttpServletRequest;

import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

@CrossOrigin
@RestController
@RequestMapping("/api/users")
public class UserControllerRest {

    private final UserService userService;

    public UserControllerRest(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody UserDTO userDTO) {
        // Server-side validation: never create accounts with missing/invalid data.
        String email = userDTO.email();
        String name = userDTO.name();
        String password = userDTO.password();
        // Each error carries a stable, machine-readable "code" alongside the English
        // "message". The UI language lives entirely in the frontend (Transloco), so the
        // client renders these codes in the active language — this is especially needed
        // for signup, an anonymous flow where the backend has no user language to rely on.
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "INVALID_EMAIL", "message", "A valid email is required"));
        }
        if (name == null || name.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "NAME_REQUIRED", "message", "Name is required"));
        }
        if (password == null || password.length() < 4) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "PASSWORD_TOO_SHORT", "message", "Password must be at least 4 characters"));
        }
        // GDPR: consent is enforced HERE, not just by the client checkbox. A stale cached
        // page (or any direct API call) that omits it is rejected, so we never create an
        // account — nor record a false proof of consent — without genuine acceptance.
        if (!Boolean.TRUE.equals(userDTO.termsAccepted())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "TERMS_NOT_ACCEPTED", "message", "You must accept the terms of use"));
        }

        // Security: public signups ALWAYS get the USER role, whatever the request says.
        // Honoring client-sent roles would let a direct API call self-register as ADMIN
        // (or with no role at all, locking the account out of every authenticated
        // endpoint). Internal callers (DatabaseInitializer, tests) keep using
        // userService.save() directly, which honors the DTO's roles.
        UserDTO sanitized = new UserDTO(null, email, name, userDTO.language(),
                userDTO.collections(), List.of("USER"), password, null,
                userDTO.termsAccepted(), null, userDTO.emailConsent());
        UserDTO newUser = userService.save(sanitized);
        if (newUser == null) {
            // 409 Conflict: an email-already-registered clash. The distinct STATUS lets the
            // client show the specific localized message even in environments where the
            // JSON error body is replaced by a generic one (as happens behind the proxy).
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("code", "EMAIL_IN_USE", "message", "Email already in use"));
        }
        URI location = fromCurrentRequest().path("/{id}").buildAndExpand(newUser.id()).toUri();
        return ResponseEntity.created(location).body(newUser);
    }

    @GetMapping("/me")
    public UserDTO me(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal != null) {
            return userService.findByEmail(principal.getName());
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(@RequestBody UserDTO data, HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDTO updated = userService.updateProfile(principal.getName(), data);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/me/check-password")
    public ResponseEntity<?> checkPassword(@RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        boolean matches = userService.checkPassword(principal.getName(), body.get("password"));
        if (matches) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Incorrect password"));
        }
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "New password cannot be empty"));
        }
        UserDTO updated = userService.changePassword(principal.getName(), newPassword);
        return ResponseEntity.ok(updated);
    }

    // ——————————————————— Self-service account deletion ———————————————————

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteOwnAccount(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.deleteOwnAccount(principal.getName());
        return ResponseEntity.noContent().build();
    }

    // ——————————————————————— Admin user management ———————————————————————

    @GetMapping
    public ResponseEntity<List<AdminUserDTO>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.listUsers(search, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserDetail(@PathVariable long id) {
        try {
            return ResponseEntity.ok(userService.getUserDetail(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable long id,
            @RequestBody AdminUserUpdateDTO data, HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        String requesterEmail = principal != null ? principal.getName() : null;
        try {
            return ResponseEntity.ok(userService.updateUserByAdmin(id, data, requesterEmail));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/blocked")
    public ResponseEntity<?> setBlocked(@PathVariable long id,
            @RequestBody Map<String, Boolean> body, HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        String requesterEmail = principal != null ? principal.getName() : null;
        Boolean blocked = body.get("blocked");
        if (blocked == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Missing 'blocked' field"));
        }
        try {
            return ResponseEntity.ok(userService.setBlocked(id, blocked, requesterEmail));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Grants/extends or revokes PREMIUM for a user. Body: {"premiumUntil": ISO-local-datetime}
     * to grant until that moment, or a null/blank value to revoke (back to free plan).
     */
    @PatchMapping("/{id}/premium")
    public ResponseEntity<?> setPremium(@PathVariable long id,
            @RequestBody Map<String, String> body) {
        if (!body.containsKey("premiumUntil")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "PREMIUM_DATE_MISSING", "message", "Missing 'premiumUntil' field"));
        }
        String raw = body.get("premiumUntil");
        LocalDateTime premiumUntil;
        try {
            // Null/blank revokes premium; otherwise parse the admin-chosen expiry.
            premiumUntil = (raw == null || raw.isBlank()) ? null : LocalDateTime.parse(raw);
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "PREMIUM_DATE_INVALID", "message", "Invalid date/time"));
        }
        try {
            return ResponseEntity.ok(userService.setPremium(id, premiumUntil));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable long id, HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        String requesterEmail = principal != null ? principal.getName() : null;
        try {
            userService.deleteUser(id, requesterEmail);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * One-click unsubscribe from reminder emails, linked from every reminder (the
     * token identifies the user — no login, per GDPR ease-of-withdrawal). Public
     * (see SecurityConfig) and answers with a tiny standalone confirmation page in
     * the user's language: the visitor comes from an email client, not the SPA.
     */
    @GetMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribe(@RequestParam String token) {
        return userService.unsubscribeByToken(token)
                .map(user -> ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(unsubscribePage("es".equalsIgnoreCase(user.getLanguage()))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.TEXT_HTML)
                        .body(unsubscribeInvalidPage()));
    }

    private String unsubscribePage(boolean es) {
        String title = es ? "Te has dado de baja" : "You have been unsubscribed";
        String body = es
                ? "No volverás a recibir recordatorios por correo. Puedes reactivarlos cuando quieras desde tu perfil."
                : "You will no longer receive email reminders. You can turn them back on anytime from your profile.";
        return unsubscribeHtml(title, body);
    }

    private String unsubscribeInvalidPage() {
        // Unknown token → unknown user → unknown language, so both are shown.
        return unsubscribeHtml("Invalid link / Enlace no válido",
                "This unsubscribe link is not valid. / Este enlace de baja no es válido.");
    }

    private String unsubscribeHtml(String title, String body) {
        return """
<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="robots" content="noindex"><title>%TITLE% · ChineseReads</title></head>
<body style="margin:0;font-family:Arial,Helvetica,sans-serif;background:#f4f1ec;display:flex;align-items:center;justify-content:center;min-height:100vh;">
<div style="background:#fff;border-radius:12px;padding:36px 40px;max-width:420px;text-align:center;">
<div style="color:#c0392b;font-size:20px;font-weight:bold;margin-bottom:14px;">ChineseReads</div>
<h1 style="font-size:19px;color:#2c3e50;margin:0 0 10px;">%TITLE%</h1>
<p style="font-size:14px;color:#555;line-height:1.6;margin:0;">%BODY%</p>
</div></body></html>
"""
                .replace("%TITLE%", title)
                .replace("%BODY%", body);
    }
}