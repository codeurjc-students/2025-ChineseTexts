package com.chinesereads.backend.Controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.ActivityService;
import com.chinesereads.backend.dto.StatsDTO;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Reading-activity endpoints behind the streak and progress stats. Registered-only:
 * anonymous reading is welcome but untracked (there is no account to attach it to).
 */
@CrossOrigin
@RestController
@RequestMapping("/api/activity")
public class ActivityControllerRest {

    private final ActivityService activityService;

    private final UserRepository userRepository;

    public ActivityControllerRest(ActivityService activityService, UserRepository userRepository) {
        this.activityService = activityService;
        this.userRepository = userRepository;
    }

    /** Logs a reading; body: {"textKey": "public:12" | "own:34"}. Idempotent per day. */
    @PostMapping("/reading")
    public ResponseEntity<?> recordReading(@RequestBody(required = false) Map<String, String> body,
                                           HttpServletRequest request) {
        User user = currentUser(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String textKey = body != null ? body.get("textKey") : null;
        if (textKey == null || textKey.isBlank() || textKey.length() > 64) {
            return ResponseEntity.badRequest().build();
        }
        activityService.recordReading(user, textKey.trim());
        return ResponseEntity.ok().build();
    }

    /** The user's progress snapshot (streak, totals, weekly chart). */
    @GetMapping("/stats")
    public ResponseEntity<StatsDTO> stats(HttpServletRequest request) {
        User user = currentUser(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(activityService.getStats(user));
    }

    private User currentUser(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            return null;
        }
        return userRepository.findByEmail(principal.getName()).orElse(null);
    }
}
