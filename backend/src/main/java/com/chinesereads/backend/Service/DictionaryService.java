package com.chinesereads.backend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chinesereads.backend.Model.Word;
import com.chinesereads.backend.Repository.WordRepository;

@Service
public class DictionaryService {

    @Autowired
    private WordRepository wordRepository;

    public Word save(Word word){
        if(wordRepository.findByChinese(word.getChinese()).isPresent()){
            return null;
        } else{
            return wordRepository.save(word);
        }
    }

}