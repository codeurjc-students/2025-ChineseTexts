package com.chinesereads.backend.Controller;

import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.chinesereads.backend.Service.TextService;
import com.chinesereads.backend.dto.TextDTO;
import com.chinesereads.backend.dto.ValidationResultDTO;

@CrossOrigin
@RestController
@RequestMapping("/api/texts")
public class TextControllerRest {

    @Autowired
    private TextService textService;

    @GetMapping
    public List<TextDTO> getTexts(@RequestParam int page, @RequestParam int size) {
        return textService.getTexts(page, size);
    }

    @GetMapping("/level/{level}")
    public List<TextDTO> getTextsByLevel(@PathVariable String level,
            @RequestParam int page, @RequestParam int size) {
        return textService.getTextsByLevel(level, page, size);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getTextImage(@PathVariable long id) throws SQLException {
        Resource image = textService.getTextImage(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
                .body(image);
    }

    @GetMapping("/{id}")
    public TextDTO getText(@PathVariable long id) {
        return textService.getText(id);
    }

    @GetMapping("/{id}/SpanishText")
    public ResponseEntity<String[][]> getTextSpanish(@PathVariable long id) {
        TextDTO text = textService.getText(id);
        if (text == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        return ResponseEntity.ok(textService.getTextSpanish(text));
    }

    @GetMapping("/{id}/EnglishText")
    public ResponseEntity<String[][]> getTextEnglish(@PathVariable long id) {
        TextDTO text = textService.getText(id);
        if (text == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        return ResponseEntity.ok(textService.getTextEnglish(text));
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidationResultDTO> validateText(@RequestParam String chineseText) {
        return ResponseEntity.ok(textService.validateText(chineseText));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadText(
            @RequestPart("data") TextDTO data,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            TextDTO saved = textService.uploadText(data, image);
            if (saved == null) return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("A text with this title already exists.");
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}