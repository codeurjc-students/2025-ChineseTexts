package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.chinesereads.backend.Model.Collection;
import com.chinesereads.backend.Model.Flashcard;
import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.FlashcardRepository;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.FlashcardService;
import com.chinesereads.backend.dto.FlashcardMapperImpl;
import com.chinesereads.backend.dto.SrsReviewResultDTO;

/**
 * SM-2 scheduling contract: the interval ladder (1 → 6 → interval × ease), the reset
 * on failure (card stays due today), the 1.3 ease floor, the "legacy NULL fields =
 * brand-new card" rule, and ownership enforcement.
 */
public class FlashcardServiceTest {

    private FlashcardRepository flashcardRepository;
    private UserRepository userRepository;
    private FlashcardService flashcardService;

    private User owner;
    private Flashcard card;

    @BeforeEach
    public void setUp() {
        flashcardRepository = mock(FlashcardRepository.class);
        userRepository = mock(UserRepository.class);
        flashcardService = new FlashcardService(flashcardRepository, null, null, null,
                userRepository, new FlashcardMapperImpl());

        owner = new User("u@u.com", "User", "encoded", "es", "USER");
        Collection collection = new Collection("Col", LocalDate.now(), owner, List.of());
        card = new Flashcard(null, null, collection);
        card.setId(1L);

        when(flashcardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(flashcardRepository.save(any(Flashcard.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // Test unitario 1: una tarjeta nueva contestada bien se programa para mañana
    @Test
    @DisplayName("A new card answered 'good' is scheduled 1 day ahead")
    public void newCardGoodAnswerSchedulesOneDay() {
        SrsReviewResultDTO result = flashcardService.reviewFlashcard(1L, 4, "u@u.com");

        assertEquals(1, result.intervalDays());
        assertEquals(LocalDate.now().plusDays(1), result.nextDue());
        assertEquals(1, card.getSrsRepetitions());
    }

    // Test unitario 2: la escalera de intervalos SM-2 (1 → 6 → 6×ease)
    @Test
    @DisplayName("Successive good answers follow the SM-2 ladder: 1, 6, then interval x ease")
    public void goodAnswersFollowSm2Ladder() {
        assertEquals(1, flashcardService.reviewFlashcard(1L, 4, "u@u.com").intervalDays());
        assertEquals(6, flashcardService.reviewFlashcard(1L, 4, "u@u.com").intervalDays());

        double easeAfterTwo = card.getSrsEase();
        int third = flashcardService.reviewFlashcard(1L, 4, "u@u.com").intervalDays();
        assertEquals(Math.round(6 * easeAfterTwo), third);
        assertTrue(third > 6);
    }

    // Test unitario 3: fallar resetea las repeticiones y la tarjeta sigue tocando hoy
    @Test
    @DisplayName("Failing a card resets repetitions and keeps it due today")
    public void failResetsAndStaysDueToday() {
        flashcardService.reviewFlashcard(1L, 4, "u@u.com");
        flashcardService.reviewFlashcard(1L, 4, "u@u.com");

        SrsReviewResultDTO result = flashcardService.reviewFlashcard(1L, 0, "u@u.com");

        assertEquals(0, result.intervalDays());
        assertEquals(LocalDate.now(), result.nextDue());
        assertEquals(0, card.getSrsRepetitions());
    }

    // Test unitario 4: el factor de facilidad nunca baja de 1.3 (suelo de SM-2)
    @Test
    @DisplayName("The ease factor never drops below SM-2's 1.3 floor")
    public void easeNeverDropsBelowFloor() {
        for (int i = 0; i < 20; i++) {
            flashcardService.reviewFlashcard(1L, 0, "u@u.com");
        }
        assertEquals(1.3, card.getSrsEase(), 0.0001);
    }

    // Test unitario 5: una tarjeta anterior a la funcionalidad (campos NULL) se trata como nueva
    @Test
    @DisplayName("A pre-feature card (NULL SRS fields) is graded as a brand-new card")
    public void legacyNullFieldsBehaveAsNewCard() {
        // card starts with all SRS fields null (no setters called)
        SrsReviewResultDTO result = flashcardService.reviewFlashcard(1L, 5, "u@u.com");

        assertEquals(1, result.intervalDays());
        assertEquals(1, card.getSrsRepetitions());
        assertTrue(card.getSrsEase() > 2.5); // easy answer raises the default 2.5
    }

    // Test unitario 6: solo el dueño de la tarjeta puede repasarla
    @Test
    @DisplayName("Reviewing someone else's card is rejected")
    public void reviewingForeignCardIsRejected() {
        assertThrows(RuntimeException.class,
                () -> flashcardService.reviewFlashcard(1L, 4, "intruder@u.com"));
    }

    // Test unitario 7: una calidad fuera de 0-5 se rechaza
    @Test
    @DisplayName("A quality outside 0-5 is rejected")
    public void invalidQualityIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> flashcardService.reviewFlashcard(1L, 6, "u@u.com"));
        assertThrows(IllegalArgumentException.class,
                () -> flashcardService.reviewFlashcard(1L, -1, "u@u.com"));
    }

    // Test unitario 8: el contador de pendientes delega en la consulta del repositorio
    @Test
    @DisplayName("getDueCount asks the repository for today's due cards")
    public void dueCountDelegatesToRepository() {
        when(userRepository.findByEmail("u@u.com")).thenReturn(Optional.of(owner));
        when(flashcardRepository.countDue(owner, LocalDate.now())).thenReturn(3L);

        assertEquals(3L, flashcardService.getDueCount("u@u.com"));
    }
}
