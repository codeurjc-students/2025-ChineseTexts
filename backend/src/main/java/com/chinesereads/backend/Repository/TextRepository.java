package com.chinesereads.backend.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.chinesereads.backend.Model.Text;

public interface TextRepository extends JpaRepository<Text, Long> {

    Optional<Text> findByTitleSpanish(String titleSpanish);

    Optional<Text> findByTitleEnglish(String titleEnglish);

    // Ya lo heredas de JpaRepository, pero puedes dejarlo si quieres
    Page<Text> findAll(Pageable pageable);

    // Nuevo: obtener textos filtrados por nivel con paginación
    Page<Text> findByLevel(String level, Pageable pageable);
}
