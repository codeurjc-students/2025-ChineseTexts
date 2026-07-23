package com.chinesereads.backend.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.chinesereads.backend.Model.Text;
import com.chinesereads.backend.Repository.TextRepository;
import com.chinesereads.backend.Service.TextService;
import com.chinesereads.backend.dto.TextDTO;

@SpringBootTest
@ActiveProfiles("test")
public class TextServiceIntegrationTest {

    @Autowired
    private TextService textService;

    @Autowired
    private TextRepository textRepository;

    @AfterEach
    public void tearDown() {
        textRepository.deleteAll();
    }

    // Test de integración 1: Cuando se guarda un texto, se persiste en la base de datos
    @Test
    @DisplayName("When a text is saved, it should be persisted in the database")
    public void testSaveTextPersisted() {
        Text text = new Text("Integration Title", "Título Integración", "你好。",
                "Hello.", "Hola.", "Desc", "Desc ES", "HSK1", LocalDate.now(), null);

        TextDTO saved = textService.save(text);

        assertNotNull(saved);
        assertNotNull(saved.id());
        assertEquals("Integration Title", saved.titleEnglish());
        assertEquals(1, textRepository.findAll().size());
    }

    // Test de integración 2: Cuando se guarda un texto con título duplicado, devuelve null
    @Test
    @DisplayName("When a text with a duplicate title is saved, it should return null")
    public void testSaveTextDuplicateTitle() {
        Text text = new Text("Dup Title", "Dup Título", "你好。",
                "Hello.", "Hola.", "Desc", "Desc ES", "HSK1", LocalDate.now(), null);

        textService.save(text);

        // Mismo título en inglés — debe devolver null
        Text duplicate = new Text("Dup Title", "Otro Título", "再见。",
                "Bye.", "Adiós.", "Desc2", "Desc2 ES", "HSK2", LocalDate.now(), null);
        TextDTO result = textService.save(duplicate);

        assertNull(result);
        // Verificamos directamente en el repositorio que solo existe un texto con ese título
        assertTrue(textRepository.findByTitleEnglish("Dup Title").isPresent());
        assertTrue(textRepository.findByTitleSpanish("Otro Título").isEmpty());
    }

    // Test de integración 3: Cuando se recupera un texto por id, se devuelve correctamente
    @Test
    @DisplayName("When a text is retrieved by id, it should return the correct text")
    public void testGetTextById() {
        Text text = new Text("Fetch Title", "Título Fetch", "你好。",
                "Hello.", "Hola.", "Desc", "Desc ES", "HSK1", LocalDate.now(), null);
        TextDTO saved = textService.save(text);

        TextDTO retrieved = textService.getText(saved.id());

        assertNotNull(retrieved);
        assertEquals("Fetch Title", retrieved.titleEnglish());
        assertEquals("HSK1", retrieved.level());
    }

    // Test de integración 4: Los textos se filtran correctamente por nivel
    @Test
    @DisplayName("When texts are filtered by level, only matching texts are returned")
    public void testGetTextsByLevel() {
        Text hsk1 = new Text("HSK1 Text", "Texto HSK1", "你好。",
                "Hello.", "Hola.", "Desc", "Desc ES", "HSK1", LocalDate.now(), null);
        Text hsk2 = new Text("HSK2 Text", "Texto HSK2", "再见。",
                "Bye.", "Adiós.", "Desc", "Desc ES", "HSK2", LocalDate.now(), null);

        textService.save(hsk1);
        textService.save(hsk2);

        var hsk1Results = textService.getTextsByLevel("HSK1", 0, 10, null);
        var hsk2Results = textService.getTextsByLevel("HSK2", 0, 10, null);

        assertEquals(1, hsk1Results.size());
        assertEquals("HSK1 Text", hsk1Results.get(0).titleEnglish());
        assertEquals(1, hsk2Results.size());
        assertEquals("HSK2 Text", hsk2Results.get(0).titleEnglish());
    }

    // Test de integración 5: las etiquetas persisten en la tabla de colección y
    // el filtro por etiqueta (solo y combinado con nivel) devuelve solo los
    // textos etiquetados — round-trip real contra la base de datos
    @Test
    @DisplayName("Topics persist and the topic filter returns only tagged texts")
    public void testTopicsPersistAndFilter() {
        Text tagged = new Text("Sports Text", "Texto Deportes", "你好。",
                "Hello.", "Hola.", "Desc", "Desc ES", "HSK1", LocalDate.now(), null);
        tagged.setTopics(new java.util.LinkedHashSet<>(java.util.List.of("sports", "health")));
        Text untagged = new Text("Plain Text", "Texto Plano", "再见。",
                "Bye.", "Adiós.", "Desc", "Desc ES", "HSK1", LocalDate.now(), null);

        textService.save(tagged);
        textService.save(untagged);

        TextDTO reloaded = textService.getText(
                textRepository.findByTitleEnglish("Sports Text").get().getId());
        assertNotNull(reloaded.topics());
        assertEquals(2, reloaded.topics().size());
        assertTrue(reloaded.topics().contains("sports"));

        var bySports = textService.getTexts(0, 10, "sports");
        assertEquals(1, bySports.size());
        assertEquals("Sports Text", bySports.get(0).titleEnglish());

        var byLevelAndTopic = textService.getTextsByLevel("HSK1", 0, 10, "sports");
        assertEquals(1, byLevelAndTopic.size());

        var noMatch = textService.getTexts(0, 10, "travel");
        assertEquals(0, noMatch.size());
    }

    // Test de integración 6: la edición de metadatos persiste las etiquetas y el
    // nivel nuevos sin tocar el contenido
    @Test
    @DisplayName("updateTextMetadata persists new topics and level")
    public void testUpdateTextMetadataPersists() {
        Text text = new Text("Edit Me", "Edítame", "你好。",
                "Hello.", "Hola.", "Desc", "Desc ES", "HSK1", LocalDate.now(), null);
        TextDTO saved = textService.save(text);

        var patch = new com.chinesereads.backend.dto.TextMetadataUpdateDTO(
                null, null, null, null, "HSK2", java.util.List.of("food", "family"));
        TextDTO updated = textService.updateTextMetadata(saved.id(), patch);

        assertNotNull(updated);
        TextDTO reloaded = textService.getText(saved.id());
        assertEquals("HSK2", reloaded.level());
        assertEquals(2, reloaded.topics().size());
        assertEquals("Edit Me", reloaded.titleEnglish());
        assertEquals("你好。", reloaded.text());
    }
}