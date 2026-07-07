package com.chinesereads.backend.Controller;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    @Autowired
    private UserService userService;

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

        UserDTO newUser = userService.save(userDTO);
        if (newUser == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
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
}