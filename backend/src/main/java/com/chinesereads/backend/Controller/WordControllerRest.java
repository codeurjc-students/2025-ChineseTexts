package com.chinesereads.backend.Controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chinesereads.backend.Service.WordService;
import com.chinesereads.backend.dto.WordDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

@CrossOrigin
@RestController
@RequestMapping("/api/words")
public class WordControllerRest {

    @Autowired
    private WordService wordService;

    @GetMapping("/textWords")
    public ResponseEntity<WordDTO[]> getTextWords(@RequestParam String text) {
        String[] originalTextArray = text.split(",");
        WordDTO[] pinyinResults = new WordDTO[originalTextArray.length];

        for (int i = 0; i < originalTextArray.length; i++) {
            pinyinResults[i] = wordService.getWord(originalTextArray[i]);
        }
        
        // Devolver el array de resultados
        return ResponseEntity.status(HttpStatus.OK).body(pinyinResults);
    }
}
