package com.chinesereads.backend.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.chinesereads.backend.Model.Text;

public interface TextRepository extends JpaRepository<Text, Long> {

    /**
     * Projection for the dynamic sitemap: just id + date, so building the sitemap
     * never loads the @Lob columns (text, translations, image) of every text.
     */
    interface TextSitemapRow {
        Long getId();
        LocalDate getCreationDate();
    }

    @Query("SELECT t.id AS id, t.creationDate AS creationDate FROM Text t ORDER BY t.id")
    List<TextSitemapRow> findSitemapRows();

    Optional<Text> findByTitleSpanish(String titleSpanish);

    Optional<Text> findByTitleEnglish(String titleEnglish);

    // Ya lo heredas de JpaRepository, pero puedes dejarlo si quieres
    Page<Text> findAll(Pageable pageable);

    // Nuevo: obtener textos filtrados por nivel con paginación
    Page<Text> findByLevel(String level, Pageable pageable);

    // Filtro por etiqueta temática (solo o combinado con el nivel). MEMBER OF
    // consulta la tabla de colección text_topic sin cargar los @Lob de más filas
    // de las que pide la página.
    @Query("SELECT t FROM Text t WHERE :topic MEMBER OF t.topics")
    Page<Text> findByTopic(String topic, Pageable pageable);

    @Query("SELECT t FROM Text t WHERE t.level = :level AND :topic MEMBER OF t.topics")
    Page<Text> findByLevelAndTopic(String level, String topic, Pageable pageable);
}
