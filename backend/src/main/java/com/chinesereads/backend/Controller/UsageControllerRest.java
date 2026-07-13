package com.chinesereads.backend.Controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.UsageService;
import com.chinesereads.backend.dto.UsageStatusDTO;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Exposes the authenticated user's own usage so the UI can show a generation meter
 * ("X of N used this month") and nudge free users toward premium. Read-only.
 */
@CrossOrigin
@RestController
@RequestMapping("/api/usage")
public class UsageControllerRest {

    private final UsageService usageService;

    private final UserRepository userRepository;

    public UsageControllerRest(UsageService usageService, UserRepository userRepository) {
        this.usageService = usageService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<UsageStatusDTO> myUsage(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(usageService.getStatus(user));
    }
}
