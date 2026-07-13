package com.chinesereads.backend.Service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.chinesereads.backend.Model.Collection;
import com.chinesereads.backend.Model.Flashcard;
import com.chinesereads.backend.Model.Text;
import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Model.Word;
import com.chinesereads.backend.Repository.CollectionRepository;
import com.chinesereads.backend.Repository.FlashcardRepository;
import com.chinesereads.backend.Repository.TextRepository;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Repository.WordRepository;
import com.chinesereads.backend.dto.FlashcardDTO;
import com.chinesereads.backend.dto.FlashcardMapper;
import com.chinesereads.backend.dto.SrsReviewResultDTO;

@Service
public class FlashcardService {

    // SM-2 defaults for a card that has never been reviewed (or predates the feature).
    private static final double DEFAULT_EASE = 2.5;
    private static final double MIN_EASE = 1.3;

    private final FlashcardRepository flashcardRepository;

    private final CollectionRepository collectionRepository;

    private final WordRepository wordRepository;

    private final TextRepository textRepository;

    private final UserRepository userRepository;

    private final FlashcardMapper flashcardMapper;

    public FlashcardService(FlashcardRepository flashcardRepository, CollectionRepository collectionRepository, WordRepository wordRepository, TextRepository textRepository, UserRepository userRepository, FlashcardMapper flashcardMapper) {
        this.flashcardRepository = flashcardRepository;
        this.collectionRepository = collectionRepository;
        this.wordRepository = wordRepository;
        this.textRepository = textRepository;
        this.userRepository = userRepository;
        this.flashcardMapper = flashcardMapper;
    }

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

    // ——————————————————————— SRS (SM-2, Anki's algorithm) ———————————————————————

    /** All the user's cards due today (across every collection), oldest due first. */
    public List<FlashcardDTO> getDueFlashcards(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return flashcardMapper.toDTO(flashcardRepository.findDue(user, LocalDate.now()));
    }

    /** How many cards are due today — cheap enough for the header badge. */
    public long getDueCount(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return flashcardRepository.countDue(user, LocalDate.now());
    }

    /**
     * Grades one card with SM-2. Quality 0-5 (the UI sends 0 = again, 3 = hard,
     * 4 = good, 5 = easy). Below 3 the card is forgotten: repetitions reset and it
     * stays due TODAY, so it keeps coming back within the session until it sticks.
     * From 3 up, the interval grows 1 day → 6 days → previous × ease, and the ease
     * factor drifts with answer quality but never below 1.3 (SM-2's floor, which
     * prevents "ease hell" where a card's interval stops growing).
     */
    public SrsReviewResultDTO reviewFlashcard(Long flashcardId, int quality, String email) {
        if (quality < 0 || quality > 5) {
            throw new IllegalArgumentException("quality must be between 0 and 5");
        }
        Flashcard card = flashcardRepository.findById(flashcardId).orElseThrow();
        if (!card.getCollection().getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized");
        }

        int repetitions = card.getSrsRepetitions() == null ? 0 : card.getSrsRepetitions();
        double ease = card.getSrsEase() == null ? DEFAULT_EASE : card.getSrsEase();
        int interval = card.getSrsIntervalDays() == null ? 0 : card.getSrsIntervalDays();

        if (quality < 3) {
            repetitions = 0;
            interval = 0;
        } else {
            if (repetitions == 0) {
                interval = 1;
            } else if (repetitions == 1) {
                interval = 6;
            } else {
                interval = (int) Math.round(interval * ease);
            }
            repetitions++;
        }

        ease = ease + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        if (ease < MIN_EASE) {
            ease = MIN_EASE;
        }

        LocalDate nextDue = LocalDate.now().plusDays(interval);
        card.setSrsRepetitions(repetitions);
        card.setSrsEase(ease);
        card.setSrsIntervalDays(interval);
        card.setSrsDueDate(nextDue);
        flashcardRepository.save(card);

        return new SrsReviewResultDTO(card.getId(), interval, nextDue);
    }
}