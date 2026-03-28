package com.chinesereads.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chinesereads.backend.Model.Collection;
import com.chinesereads.backend.Model.Flashcard;
import com.chinesereads.backend.Model.Word;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {
    boolean existsByWordAndCollection(Word word, Collection collection);
}