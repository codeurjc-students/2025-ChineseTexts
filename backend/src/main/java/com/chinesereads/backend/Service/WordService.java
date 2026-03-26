package com.chinesereads.backend.Service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chinesereads.backend.Model.Word;
import com.chinesereads.backend.Repository.WordRepository;
import com.chinesereads.backend.dto.WordDTO;
import com.chinesereads.backend.dto.WordMapper;

@Service
public class WordService {

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private WordMapper wordMapper;

    public WordDTO getWord(String chinese) {
        Optional<Word> word = wordRepository.findByChinese(chinese);
        if (word.isPresent()) {
            return wordMapper.toDTO(word.get());
        } else {
            return null;
        }
    }

}