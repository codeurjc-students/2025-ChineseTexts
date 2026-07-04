package com.chinesereads.backend.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.chinesereads.backend.Service.FounderService;
import com.chinesereads.backend.dto.FounderItemDTO;
import com.chinesereads.backend.dto.FounderProfileDTO;
import com.chinesereads.backend.dto.FounderSectionDTO;
import com.chinesereads.backend.dto.FounderSocialDTO;

/**
 * API del perfil del creador (/api/founder).
 *
 * Los GET son públicos (los ve cualquier visitante); las escrituras
 * (POST/PUT/DELETE) están restringidas a ADMIN en {@code SecurityConfig}.
 */
@CrossOrigin
@RestController
@RequestMapping("/api/founder")
public class FounderControllerRest {

    @Autowired
    private FounderService founderService;

    // ---------- Perfil ----------

    @GetMapping
    public FounderProfileDTO getProfile() {
        return founderService.getProfile();
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody FounderProfileDTO data) {
        try {
            return ResponseEntity.ok(founderService.updateProfile(data));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ---------- Foto de perfil ----------

    @GetMapping("/photo")
    public ResponseEntity<Resource> getPhoto() {
        try {
            Resource photo = founderService.getPhoto();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
                    .body(photo);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/photo")
    public ResponseEntity<?> setPhoto(@RequestParam("image") MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No image provided"));
        }
        try {
            founderService.setPhoto(image);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/photo")
    public ResponseEntity<?> deletePhoto() {
        founderService.deletePhoto();
        return ResponseEntity.noContent().build();
    }

    // ---------- Enlaces / redes ----------

    @PostMapping("/socials")
    public ResponseEntity<?> addSocial(@RequestBody FounderSocialDTO data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(founderService.addSocial(data));
    }

    @PutMapping("/socials/{id}")
    public ResponseEntity<?> updateSocial(@PathVariable long id, @RequestBody FounderSocialDTO data) {
        try {
            return ResponseEntity.ok(founderService.updateSocial(id, data));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/socials/{id}")
    public ResponseEntity<?> deleteSocial(@PathVariable long id) {
        try {
            founderService.deleteSocial(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // ---------- Secciones ----------

    @PostMapping("/sections")
    public ResponseEntity<?> addSection(@RequestBody FounderSectionDTO data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(founderService.addSection(data));
    }

    @PutMapping("/sections/{id}")
    public ResponseEntity<?> updateSection(@PathVariable long id, @RequestBody FounderSectionDTO data) {
        try {
            return ResponseEntity.ok(founderService.updateSection(id, data));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/sections/{id}")
    public ResponseEntity<?> deleteSection(@PathVariable long id) {
        try {
            founderService.deleteSection(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // ---------- Ítems ----------

    @PostMapping("/sections/{sectionId}/items")
    public ResponseEntity<?> addItem(@PathVariable long sectionId, @RequestBody FounderItemDTO data) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(founderService.addItem(sectionId, data));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<?> updateItem(@PathVariable long id, @RequestBody FounderItemDTO data) {
        try {
            return ResponseEntity.ok(founderService.updateItem(id, data));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable long id) {
        try {
            founderService.deleteItem(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // ---------- Logo de un ítem ----------

    @GetMapping("/items/{id}/logo")
    public ResponseEntity<Resource> getItemLogo(@PathVariable long id) {
        try {
            Resource logo = founderService.getItemLogo(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
                    .body(logo);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/items/{id}/logo")
    public ResponseEntity<?> setItemLogo(@PathVariable long id, @RequestParam("image") MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No image provided"));
        }
        try {
            founderService.setItemLogo(id, image);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/items/{id}/logo")
    public ResponseEntity<?> deleteItemLogo(@PathVariable long id) {
        try {
            founderService.deleteItemLogo(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
