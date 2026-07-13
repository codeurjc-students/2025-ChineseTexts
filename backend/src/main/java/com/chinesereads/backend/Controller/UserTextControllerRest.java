package com.chinesereads.backend.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.chinesereads.backend.Service.UsageLimitException;
import com.chinesereads.backend.Service.UserTextService;
import com.chinesereads.backend.dto.UserTextReaderDTO;
import com.chinesereads.backend.dto.UserTextSummaryDTO;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Private per-user texts. Every endpoint requires an authenticated user (enforced
 * in SecurityConfig) and is scoped to the caller — a user can only see and manage
 * their own texts.
 */
@CrossOrigin
@RestController
@RequestMapping("/api/my-texts")
public class UserTextControllerRest {

    private final UserTextService userTextService;

    public UserTextControllerRest(UserTextService userTextService) {
        this.userTextService = userTextService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestParam(value = "text", required = false) String text,
            @RequestPart(value = "image", required = false) MultipartFile image,
            HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            UserTextSummaryDTO created = userTextService.create(principal.getName(), text, image);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (UsageLimitException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Could not process the text. Please try again."));
        }
    }

    /** OCR "extract" step: image -> extracted Chinese text (with layout) for review/edit. */
    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> extract(
            @RequestPart(value = "image") MultipartFile image,
            HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            String text = userTextService.extract(principal.getName(), image);
            return ResponseEntity.ok(Map.of("text", text));
        } catch (UsageLimitException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Could not read the image. Please try another photo."));
        }
    }

    @GetMapping
    public ResponseEntity<List<UserTextSummaryDTO>> listMine(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(userTextService.listMine(principal.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMine(@PathVariable long id, HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            UserTextReaderDTO reader = userTextService.getMine(id, principal.getName());
            return ResponseEntity.ok(reader);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Text not found"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMine(@PathVariable long id, HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            userTextService.deleteMine(id, principal.getName());
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Text not found"));
        }
    }
}
