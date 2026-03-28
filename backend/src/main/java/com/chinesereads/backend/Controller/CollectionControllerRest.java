package com.chinesereads.backend.Controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.chinesereads.backend.Service.CollectionService;
import com.chinesereads.backend.Service.FlashcardService;
import com.chinesereads.backend.dto.CollectionDTO;
import com.chinesereads.backend.dto.FlashcardDTO;

import jakarta.servlet.http.HttpServletRequest;

@CrossOrigin
@RestController
@RequestMapping("/api/collections")
public class CollectionControllerRest {

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private FlashcardService flashcardService;

    // GET /api/collections — colecciones del usuario logueado
    @GetMapping
    public ResponseEntity<List<CollectionDTO>> getUserCollections(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(collectionService.getUserCollections(principal.getName()));
    }

    // POST /api/collections — crear colección
    @PostMapping
    public ResponseEntity<CollectionDTO> createCollection(
            @RequestParam String title,
            HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collectionService.createCollection(title, principal.getName()));
    }

    // DELETE /api/collections/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollection(@PathVariable Long id, HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        collectionService.deleteCollection(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    // POST /api/collections/{id}/flashcards — guardar palabra en colección
    @PostMapping("/{id}/flashcards")
    public ResponseEntity<?> addFlashcard(
            @PathVariable Long id,
            @RequestParam String chinese,
            @RequestParam Long textId,
            HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            FlashcardDTO flashcard = flashcardService.addFlashcard(id, chinese, textId, principal.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(flashcard);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    // DELETE /api/collections/flashcards/{flashcardId}
    @DeleteMapping("/flashcards/{flashcardId}")
    public ResponseEntity<Void> deleteFlashcard(@PathVariable Long flashcardId, HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        flashcardService.deleteFlashcard(flashcardId, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/flashcards")
    public ResponseEntity<List<FlashcardDTO>> getCollectionFlashcards(
            @PathVariable Long id,
            HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(collectionService.getCollectionFlashcards(id, principal.getName()));
    }
}