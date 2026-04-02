package com.chinesereads.backend.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chinesereads.backend.Model.Word;
import com.chinesereads.backend.Service.DictionaryService;
import com.chinesereads.backend.Service.WordService;
import com.chinesereads.backend.dto.WordDTO;
import com.chinesereads.backend.dto.WordMapper;

@CrossOrigin
@RestController
@RequestMapping("/api/words")
public class WordControllerRest {

    @Autowired
    private WordService wordService;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private WordMapper wordMapper;

    @GetMapping("/textWords")
    public ResponseEntity<WordDTO[]> getTextWords(@RequestParam String text) {
        String[] originalTextArray = text.split("\\|");
        WordDTO[] pinyinResults = new WordDTO[originalTextArray.length];
        for (int i = 0; i < originalTextArray.length; i++) {
            pinyinResults[i] = wordService.getWord(originalTextArray[i]);
        }
        return ResponseEntity.status(HttpStatus.OK).body(pinyinResults);
    }

    @PostMapping
    public ResponseEntity<?> saveWord(@RequestBody WordDTO wordDTO) {
        Word word = new Word(wordDTO.chinese(), wordDTO.pinyin(), wordDTO.english(), wordDTO.spanish());
        Word saved = dictionaryService.save(word);
        if (saved == null) return ResponseEntity.status(HttpStatus.CONFLICT).body("Word already exists.");
        return ResponseEntity.status(HttpStatus.CREATED).body(wordMapper.toDTO(saved));
    }
}