package com.chinesereads.backend.Controller;

import org.springframework.web.bind.annotation.RestController;

import com.chinesereads.backend.Service.TextService;
import com.chinesereads.backend.dto.TextDTO;

import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@CrossOrigin
@RestController
@RequestMapping("/api/texts")
public class TextControllerRest {

    @Autowired
    private TextService textService;

    // Obtener textos sin filtro (paginados)
    @GetMapping
    public List<TextDTO> getTexts(
            @RequestParam int page,
            @RequestParam int size) {
        return textService.getTexts(page, size);
    }

    // Obtener textos filtrados por nivel (paginados)
    @GetMapping("/level/{level}")
    public List<TextDTO> getTextsByLevel(
            @PathVariable String level,
            @RequestParam int page,
            @RequestParam int size) {
        return textService.getTextsByLevel(level, page, size);
    }

    // Obtener imagen del texto
    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getProfileImage(@PathVariable long id) throws SQLException {
        Resource profileImage = textService.getTextImage(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
                .body(profileImage);
    }

    @GetMapping("/{id}")
    public TextDTO getText(@PathVariable long id) {
        return textService.getText(id);
    }

    @GetMapping("/{id}/SpanishText")
    public ResponseEntity<String[][]> getTextSpanish(@PathVariable long id){
        TextDTO text = this.textService.getText(id);
        if (text == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } else {
            String [][] result = textService.getTextSpanish(text);
            // Devolver el Map con el mapeo
            return ResponseEntity.status(HttpStatus.OK).body(result);
        }
    }

    @GetMapping("/{id}/EnglishText")
    public ResponseEntity<String[][]> getTextEnglish(@PathVariable long id){
        TextDTO text = this.textService.getText(id);
        if (text == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } else {
            String [][] result = textService.getTextEnglish(text);
            // Devolver el Map con el mapeo
            return ResponseEntity.status(HttpStatus.OK).body(result);
        }
    }
}
