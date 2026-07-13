package com.chinesereads.backend.Service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.chinesereads.backend.Model.Word;
import com.chinesereads.backend.Repository.WordRepository;
import com.chinesereads.backend.dto.WordDTO;
import com.chinesereads.backend.dto.WordMapper;

@Service
public class WordService {

    private final WordRepository wordRepository;

    private final WordMapper wordMapper;

    public WordService(WordRepository wordRepository, WordMapper wordMapper) {
        this.wordRepository = wordRepository;
        this.wordMapper = wordMapper;
    }

    public WordDTO getWord(String chinese) {
        Optional<Word> word = wordRepository.findByChinese(chinese);
        if (word.isPresent()) {
            return wordMapper.toDTO(word.get());
        } else {
            return null;
        }
    }

}