package com.chinesereads.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chinesereads.backend.Model.Flashcard;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long>{

}
