package com.chinesereads.backend.Controller;

import java.net.URI;
import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.chinesereads.backend.Service.UserService;
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
        UserDTO newUser = userService.save(userDTO);
        if (newUser == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Email already in use"));
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
}