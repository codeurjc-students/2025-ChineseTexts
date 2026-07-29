package com.chinesereads.backend.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.chinesereads.backend.Service.HallOfFameService;
import com.chinesereads.backend.dto.HallOfFameEntryDTO;
import com.chinesereads.backend.dto.HallOfFameSocialDTO;

/**
 * API del Hall of Fame (/api/hall-of-fame).
 *
 * Los GET son públicos (los ve cualquier visitante); las escrituras
 * (POST/PUT/DELETE) están restringidas a ADMIN en {@code SecurityConfig}.
 * No confundir con /api/influencers (códigos de descuento Stripe, admin-only).
 */
@CrossOrigin
@RestController
@RequestMapping("/api/hall-of-fame")
public class HallOfFameControllerRest {

    private final HallOfFameService hallOfFameService;

    public HallOfFameControllerRest(HallOfFameService hallOfFameService) {
        this.hallOfFameService = hallOfFameService;
    }

    // ---------- Entradas ----------

    @GetMapping
    public List<HallOfFameEntryDTO> getAll() {
        return hallOfFameService.getAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody HallOfFameEntryDTO data) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(hallOfFameService.create(data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable long id, @RequestBody HallOfFameEntryDTO data) {
        try {
            return ResponseEntity.ok(hallOfFameService.update(id, data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        try {
            hallOfFameService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // ---------- Foto ----------

    @GetMapping("/{id}/photo")
    public ResponseEntity<Resource> getPhoto(@PathVariable long id) {
        try {
            Resource photo = hallOfFameService.getPhoto(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
                    .body(photo);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/photo")
    public ResponseEntity<?> setPhoto(@PathVariable long id, @RequestParam("image") MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No image provided"));
        }
        try {
            hallOfFameService.setPhoto(id, image);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/photo")
    public ResponseEntity<?> deletePhoto(@PathVariable long id) {
        try {
            hallOfFameService.deletePhoto(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // ---------- Redes sociales ----------

    @PostMapping("/{id}/socials")
    public ResponseEntity<?> addSocial(@PathVariable long id, @RequestBody HallOfFameSocialDTO data) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(hallOfFameService.addSocial(id, data));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/socials/{id}")
    public ResponseEntity<?> updateSocial(@PathVariable long id, @RequestBody HallOfFameSocialDTO data) {
        try {
            return ResponseEntity.ok(hallOfFameService.updateSocial(id, data));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/socials/{id}")
    public ResponseEntity<?> deleteSocial(@PathVariable long id) {
        try {
            hallOfFameService.deleteSocial(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
