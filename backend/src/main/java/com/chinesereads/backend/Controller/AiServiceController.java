package com.chinesereads.backend.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.chinesereads.backend.Service.AiService;

import jakarta.servlet.http.HttpServletRequest;

import java.security.Principal;

@CrossOrigin
@RestController
@RequestMapping("/api/ai")
public class AiServiceController {

    @Autowired
    private AiService aiService;

    // Genera un texto chino con IA dado un nivel HSK
    @PostMapping("/generate")
    public ResponseEntity<?> generateText(
            @RequestParam String level,
            HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) return ResponseEntity.status(401).build();
        try {
            Map<String, Object> result = aiService.generateFullText(level);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // Procesa una imagen con OCR y luego genera el texto completo con IA
    @PostMapping("/ocr")
    public ResponseEntity<?> processOcr(
            @RequestPart("image") MultipartFile image,
            HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) return ResponseEntity.status(401).build();
        try {
            Map<String, Object> result = aiService.processOcrAndGenerate(image);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}