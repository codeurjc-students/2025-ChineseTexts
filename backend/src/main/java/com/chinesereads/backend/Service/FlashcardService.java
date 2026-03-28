package com.chinesereads.backend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chinesereads.backend.Model.Collection;
import com.chinesereads.backend.Model.Flashcard;
import com.chinesereads.backend.Model.Text;
import com.chinesereads.backend.Model.Word;
import com.chinesereads.backend.Repository.CollectionRepository;
import com.chinesereads.backend.Repository.FlashcardRepository;
import com.chinesereads.backend.Repository.TextRepository;
import com.chinesereads.backend.Repository.WordRepository;
import com.chinesereads.backend.dto.FlashcardDTO;
import com.chinesereads.backend.dto.FlashcardMapper;

@Service
public class FlashcardService {

    @Autowired
    private FlashcardRepository flashcardRepository;

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private TextRepository textRepository;

    @Autowired
    private FlashcardMapper flashcardMapper;

    public FlashcardDTO addFlashcard(Long collectionId, String chinese, Long textId, String email) {
        Collection collection = collectionRepository.findById(collectionId).orElseThrow();

        if (!collection.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized");
        }

        Word word = wordRepository.findByChinese(chinese)
                .orElseThrow(() -> new RuntimeException("Word not found: " + chinese));

        Text text = textRepository.findById(textId).orElseThrow();

        // Evitar duplicados en la misma colección
        boolean alreadyExists = flashcardRepository
                .existsByWordAndCollection(word, collection);
        if (alreadyExists) {
            throw new RuntimeException("Word already in collection");
        }

        Flashcard flashcard = new Flashcard(word, text, collection);
        return flashcardMapper.toDTO(flashcardRepository.save(flashcard));
    }

    public void deleteFlashcard(Long flashcardId, String email) {
        Flashcard flashcard = flashcardRepository.findById(flashcardId).orElseThrow();
        if (!flashcard.getCollection().getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized");
        }
        flashcardRepository.deleteById(flashcardId);
    }
}