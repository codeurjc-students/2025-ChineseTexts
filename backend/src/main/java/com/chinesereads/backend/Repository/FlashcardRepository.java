package com.chinesereads.backend.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chinesereads.backend.Model.Collection;
import com.chinesereads.backend.Model.Flashcard;
import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Model.Word;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {
    boolean existsByWordAndCollection(Word word, Collection collection);
    boolean existsByWord(Word word);

    // A NULL due date means the card predates the SRS feature: it is due immediately,
    // exactly like a brand-new card.
    @Query("SELECT f FROM Flashcard f WHERE f.collection.user = :user AND (f.srsDueDate IS NULL OR f.srsDueDate <= :day) ORDER BY f.srsDueDate ASC")
    List<Flashcard> findDue(@Param("user") User user, @Param("day") LocalDate day);

    @Query("SELECT COUNT(f) FROM Flashcard f WHERE f.collection.user = :user AND (f.srsDueDate IS NULL OR f.srsDueDate <= :day)")
    long countDue(@Param("user") User user, @Param("day") LocalDate day);

    /** Of the given users, the ones who saved at least one flashcard (activation metric). */
    @Query("SELECT DISTINCT f.collection.user.id FROM Flashcard f WHERE f.collection.user.id IN :userIds")
    Set<Long> findUserIdsWithAnyCard(@Param("userIds") java.util.Collection<Long> userIds);
}
