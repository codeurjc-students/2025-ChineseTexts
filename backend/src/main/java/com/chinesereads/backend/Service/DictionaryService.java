package com.chinesereads.backend.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.chinesereads.backend.Model.Word;
import com.chinesereads.backend.Repository.FlashcardRepository;
import com.chinesereads.backend.Repository.WordRepository;

@Service
public class DictionaryService {

    /** Result of a delete attempt, so the controller can map it to the right HTTP status. */
    public enum DeleteResult { DELETED, NOT_FOUND, IN_USE }

    private final WordRepository wordRepository;

    private final FlashcardRepository flashcardRepository;

    public DictionaryService(WordRepository wordRepository, FlashcardRepository flashcardRepository) {
        this.wordRepository = wordRepository;
        this.flashcardRepository = flashcardRepository;
    }

    public Word save(Word word){
        if(wordRepository.findByChinese(word.getChinese()).isPresent()){
            return null;
        } else{
            return wordRepository.save(word);
        }
    }

    /** Looks up a single word by its Chinese characters. Returns null if it does not exist. */
    public Word findByChinese(String chinese){
        return wordRepository.findByChinese(chinese).orElse(null);
    }

    /**
     * Updates the pinyin/english/spanish of an existing word. The Chinese characters act as the
     * word identity and are intentionally left unchanged. Returns null if the word does not exist.
     */
    public Word update(Long id, Word data){
        Optional<Word> optional = wordRepository.findById(id);
        if(optional.isEmpty()){
            return null;
        }
        Word word = optional.get();
        word.setPinyin(data.getPinyin());
        word.setEnglish(data.getEnglish());
        word.setSpanish(data.getSpanish());
        return wordRepository.save(word);
    }

    /**
     * Deletes a word. A word that is still referenced by any flashcard is kept, so users never
     * lose data silently; the controller turns that into a 409 CONFLICT.
     */
    public DeleteResult delete(Long id){
        Optional<Word> optional = wordRepository.findById(id);
        if(optional.isEmpty()){
            return DeleteResult.NOT_FOUND;
        }
        if(flashcardRepository.existsByWord(optional.get())){
            return DeleteResult.IN_USE;
        }
        wordRepository.deleteById(id);
        return DeleteResult.DELETED;
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