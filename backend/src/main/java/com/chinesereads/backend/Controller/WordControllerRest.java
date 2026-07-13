package com.chinesereads.backend.Controller;

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

import com.chinesereads.backend.Model.Word;
import com.chinesereads.backend.Service.DictionaryService;
import com.chinesereads.backend.Service.WordService;
import com.chinesereads.backend.dto.WordDTO;
import com.chinesereads.backend.dto.WordMapper;

@CrossOrigin
@RestController
@RequestMapping("/api/words")
public class WordControllerRest {

    private final WordService wordService;

    private final DictionaryService dictionaryService;

    private final WordMapper wordMapper;

    public WordControllerRest(WordService wordService, DictionaryService dictionaryService, WordMapper wordMapper) {
        this.wordService = wordService;
        this.dictionaryService = dictionaryService;
        this.wordMapper = wordMapper;
    }

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

    /** Admin dictionary lookup of a single word by its Chinese characters. */
    @GetMapping("/dictionary")
    public ResponseEntity<?> getDictionaryWord(@RequestParam String chinese) {
        Word word = dictionaryService.findByChinese(chinese);
        if (word == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Word not found.");
        return ResponseEntity.status(HttpStatus.OK).body(wordMapper.toDTO(word));
    }

    /** Updates an existing word (pinyin/english/spanish). The Chinese characters are the identity. */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateWord(@PathVariable Long id, @RequestBody WordDTO wordDTO) {
        Word data = new Word(wordDTO.chinese(), wordDTO.pinyin(), wordDTO.english(), wordDTO.spanish());
        Word updated = dictionaryService.update(id, data);
        if (updated == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Word not found.");
        return ResponseEntity.status(HttpStatus.OK).body(wordMapper.toDTO(updated));
    }

    /** Deletes a word, unless it is still used by flashcards (then 409 CONFLICT). */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWord(@PathVariable Long id) {
        DictionaryService.DeleteResult result = dictionaryService.delete(id);
        switch (result) {
            case NOT_FOUND:
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Word not found.");
            case IN_USE:
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("This word is used in existing flashcards and cannot be deleted.");
            default:
                return ResponseEntity.noContent().build();
        }
    }
}