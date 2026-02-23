package com.chinesereads.backend.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chinesereads.backend.Model.Word;

public interface WordRepository extends JpaRepository<Word, Long>{
    Optional<Word> findByChinese(String chinese);
}
