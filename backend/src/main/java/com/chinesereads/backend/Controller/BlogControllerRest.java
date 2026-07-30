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

import com.chinesereads.backend.Service.BlogService;
import com.chinesereads.backend.dto.BlogPostDTO;
import com.chinesereads.backend.dto.BlogPostSummaryDTO;
import com.chinesereads.backend.dto.BlogPostUpsertDTO;

/**
 * API del blog. Lectura pública (solo posts publicados) y escritura ADMIN;
 * la autorización vive en SecurityConfig: GETs públicos exactos + catch-all
 * /api/blog/** ADMIN para todo lo demás (/all, /{id}, writes, imágenes).
 */
@CrossOrigin
@RestController
@RequestMapping("/api/blog")
public class BlogControllerRest {

    private final BlogService blogService;

    public BlogControllerRest(BlogService blogService) {
        this.blogService = blogService;
    }

    // ---------- Lectura pública ----------

    @GetMapping
    public ResponseEntity<List<BlogPostSummaryDTO>> listPublished() {
        return ResponseEntity.ok(blogService.getPublishedSummaries());
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<BlogPostDTO> getBySlug(@PathVariable String slug) {
        try {
            return ResponseEntity.ok(blogService.getPublishedBySlug(slug));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<Resource> getCover(@PathVariable long id) {
        try {
            Resource cover = blogService.getCover(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
                    .body(cover);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable long id) {
        try {
            Resource image = blogService.getImage(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
                    .body(image);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ---------- Admin ----------

    @GetMapping("/all")
    public ResponseEntity<List<BlogPostSummaryDTO>> listAll() {
        return ResponseEntity.ok(blogService.getAllSummaries());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getById(@PathVariable long id) {
        try {
            return ResponseEntity.ok(blogService.getById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Object> create(@RequestBody BlogPostUpsertDTO data) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(blogService.create(data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Could not create the post"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable long id, @RequestBody BlogPostUpsertDTO data) {
        try {
            return ResponseEntity.ok(blogService.update(id, data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable long id) {
        try {
            blogService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/cover")
    public ResponseEntity<Object> setCover(@PathVariable long id,
                                           @RequestParam("image") MultipartFile image) {
        try {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Image file is required"));
            }
            blogService.setCover(id, image);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/cover")
    public ResponseEntity<Object> deleteCover(@PathVariable long id) {
        try {
            blogService.deleteCover(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/images")
    public ResponseEntity<Object> addImage(@RequestParam("image") MultipartFile image,
                                           @RequestParam(value = "postId", required = false) Long postId) {
        try {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Image file is required"));
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(blogService.addImage(image, postId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Could not upload the image"));
        }
    }
}
