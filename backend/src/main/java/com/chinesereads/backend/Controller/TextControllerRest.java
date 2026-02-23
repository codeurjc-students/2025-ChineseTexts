package com.chinesereads.backend.Controller;

import org.springframework.web.bind.annotation.RestController;

import com.chinesereads.backend.Service.TextService;
import com.chinesereads.backend.dto.TextDTO;

import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
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

    @GetMapping public List<TextDTO> getTexts(@RequestParam int page, @RequestParam int size ) { 
        return textService.getTexts(page, size);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getProfileImage(@PathVariable long id) throws SQLException {
        Resource profileImage = textService.getTextImage(id);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "image/jpeg").body(profileImage);
    }
}