package com.chinesereads.backend.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    public List<String> translateToEnglish(List<String> chineseText){
        List<String> translatedText = new ArrayList<>();
        for(String word : chineseText){
            Optional<Word> optional = wordRepository.findByChinese(word);
            if(optional.isPresent()){
                translatedText.add(optional.get().getEnglish());
            } else {
                translatedText.add("");
            }
        }
        return translatedText;
    }

    public List<String> translateToSpanish(List<String> chineseText){
        List<String> translatedText = new ArrayList<>();
        for(String word : chineseText){
            Optional<Word> optional = wordRepository.findByChinese(word);
            if(optional.isPresent()){
                translatedText.add(optional.get().getSpanish());
            } else {
                translatedText.add("");
            }
        }
        return translatedText;
    }

}