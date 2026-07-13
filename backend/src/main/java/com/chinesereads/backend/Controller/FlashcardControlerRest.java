package com.chinesereads.backend.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chinesereads.backend.Service.FlashcardService;
import com.chinesereads.backend.dto.FlashcardDTO;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Spaced-repetition endpoints (SM-2). The review queue is global — every card the
 * user owns, whatever its collection — because memory does not care which folder a
 * word lives in.
 */
@CrossOrigin
@RestController
@RequestMapping("/api/flashcards")
public class FlashcardControlerRest {

    private final FlashcardService flashcardService;

    public FlashcardControlerRest(FlashcardService flashcardService) {
        this.flashcardService = flashcardService;
    }

    /** Cards due today, oldest first (never-reviewed cards count as due). */
    @GetMapping("/due")
    public ResponseEntity<List<FlashcardDTO>> due(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(flashcardService.getDueFlashcards(principal.getName()));
    }

    /** Just the number of cards due today — for the header badge. */
    @GetMapping("/due/count")
    public ResponseEntity<?> dueCount(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of("count", flashcardService.getDueCount(principal.getName())));
    }

    /** Grades one card; body: {"quality": 0-5} (0 again, 3 hard, 4 good, 5 easy). */
    @PostMapping("/{id}/review")
    public ResponseEntity<?> review(@PathVariable Long id,
            @RequestBody Map<String, Integer> body, HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Integer quality = body.get("quality");
        if (quality == null || quality < 0 || quality > 5) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "quality must be between 0 and 5"));
        }
        try {
            return ResponseEntity.ok(flashcardService.reviewFlashcard(id, quality, principal.getName()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Flashcard not found"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Unauthorized"));
        }
    }
}
