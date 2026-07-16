package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.chinesereads.backend.Model.Text;
import com.chinesereads.backend.Model.Word;
import com.chinesereads.backend.Repository.TextRepository;
import com.chinesereads.backend.Repository.WordRepository;
import com.chinesereads.backend.Service.DictionaryService;
import com.chinesereads.backend.Service.JiebaService;
import com.chinesereads.backend.Service.TextService;
import com.chinesereads.backend.dto.TextDTO;
import com.chinesereads.backend.dto.TextMapper;
import com.chinesereads.backend.dto.TextMapperImpl;

public class TextServiceTest {

    private TextRepository textRepository;
    private WordRepository wordRepository;
    private TextService textService;
    private TextMapper textMapper;
    private JiebaService jiebaService;
    private DictionaryService dictionaryService;

    @BeforeEach
    public void setUp() {
        textRepository = mock(TextRepository.class);
        wordRepository = mock(WordRepository.class);
        jiebaService = mock(JiebaService.class);
        dictionaryService = mock(DictionaryService.class);
        textMapper = new TextMapperImpl();
        textService = new TextService(textRepository, textMapper, jiebaService, dictionaryService, wordRepository);
    }

    // Test unitario 1: Cuando se guarda un texto con título único, se guarda correctamente
    @Test
    @DisplayName("When a text with a unique title is saved, it should be persisted")
    public void testSaveTextSuccess() {
        Text text = new Text("My Title", "Mi Título", "你好。", "Hello.", "Hola.",
                "Description", "Descripción", "HSK1", LocalDate.now(), null);

        when(textRepository.findByTitleEnglish("My Title")).thenReturn(Optional.empty());
        when(textRepository.findByTitleSpanish("Mi Título")).thenReturn(Optional.empty());
        when(textRepository.save(any(Text.class))).thenReturn(text);

        TextDTO result = textService.save(text);

        assertNotNull(result);
        assertEquals("My Title", result.titleEnglish());
        verify(textRepository, times(1)).save(any(Text.class));
    }

    // Test unitario 2: Cuando se guarda un texto con título en inglés duplicado, devuelve null
    @Test
    @DisplayName("When a text with a duplicate English title is saved, it should return null")
    public void testSaveTextDuplicateEnglishTitle() {
        Text text = new Text("Existing Title", "Nuevo Título", "你好。", "Hello.", "Hola.",
                "Description", "Descripción", "HSK1", LocalDate.now(), null);

        when(textRepository.findByTitleEnglish("Existing Title"))
                .thenReturn(Optional.of(text));

        TextDTO result = textService.save(text);

        assertNull(result);
        verify(textRepository, never()).save(any(Text.class));
    }

    // Test unitario 3: Cuando se busca un texto por id existente, se devuelve el DTO
    @Test
    @DisplayName("When a text is retrieved by an existing id, it should return the DTO")
    public void testGetTextByIdFound() {
        Text text = new Text("Title", "Título", "你好。", "Hello.", "Hola.",
                "Description", "Descripción", "HSK1", LocalDate.now(), null);

        when(textRepository.findById(1L)).thenReturn(Optional.of(text));

        TextDTO result = textService.getText(1L);

        assertNotNull(result);
        assertEquals("Title", result.titleEnglish());
    }

    // Test unitario 4: Cuando se busca un texto por id inexistente, devuelve null
    @Test
    @DisplayName("When a text is retrieved by a non-existing id, it should return null")
    public void testGetTextByIdNotFound() {
        when(textRepository.findById(99L)).thenReturn(Optional.empty());

        TextDTO result = textService.getText(99L);

        assertNull(result);
    }

    // Test unitario 5: Cuando se valida un texto sin palabras faltantes, el resultado es válido
    @Test
    @DisplayName("When all words of a text exist in the dictionary, validation should pass")
    public void testValidateTextAllWordsPresent() {
        when(jiebaService.segment("你好。")).thenReturn(java.util.List.of("你好", "。"));
        when(wordRepository.findByChinese("你好")).thenReturn(Optional.of(new Word()));
        when(wordRepository.findByChinese("。")).thenReturn(Optional.of(new Word()));

        var result = textService.validateText("你好。");

        assertNotNull(result);
        assertEquals(true, result.valid());
        assertEquals(0, result.missingWords().size());
    }

    // Test unitario 6: Cuando se valida un texto con palabras faltantes, el resultado es inválido
    @Test
    @DisplayName("When a text has words missing from the dictionary, validation should fail")
    public void testValidateTextMissingWords() {
        when(jiebaService.segment("你好世界。")).thenReturn(java.util.List.of("你好", "世界", "。"));
        when(wordRepository.findByChinese("你好")).thenReturn(Optional.of(new Word()));
        when(wordRepository.findByChinese("世界")).thenReturn(Optional.empty());
        when(wordRepository.findByChinese("。")).thenReturn(Optional.of(new Word()));

        var result = textService.validateText("你好世界。");

        assertNotNull(result);
        assertEquals(false, result.valid());
        assertEquals(1, result.missingWords().size());
        assertEquals("世界", result.missingWords().get(0));
    }

    // Test unitario 7: La validación ignora los saltos de línea (una palabra partida
    // por un salto cosmético de la foto se valida entera)
    @Test
    @DisplayName("Validation strips line breaks before segmenting, so a wrapped word stays whole")
    public void testValidateTextStripsLineBreaks() {
        when(jiebaService.segment("你好。")).thenReturn(java.util.List.of("你好", "。"));
        when(wordRepository.findByChinese("你好")).thenReturn(Optional.of(new Word()));
        when(wordRepository.findByChinese("。")).thenReturn(Optional.of(new Word()));

        var result = textService.validateText("你\n好。");

        assertEquals(true, result.valid());
        verify(jiebaService, times(1)).segment("你好。");
    }

    // Test unitario 9: uploadText construye y guarda los pares de frases alineados
    // (chino↔EN↔ES) validando los recuentos — la garantía de que el lector nunca
    // vuelve a emparejar por heurística
    @Test
    @DisplayName("uploadText stores aligned sentence pairs when counts match")
    public void testUploadTextStoresAlignedSentences() throws Exception {
        TextService service = new TextService(textRepository, textMapper,
                new com.chinesereads.backend.Service.JiebaService(), dictionaryService, wordRepository);
        when(textRepository.findByTitleEnglish(any())).thenReturn(Optional.empty());
        when(textRepository.findByTitleSpanish(any())).thenReturn(Optional.empty());
        when(textRepository.save(any(Text.class))).thenAnswer(inv -> inv.getArgument(0));

        TextDTO data = new TextDTO(null, "Dialogue", "Diálogo", "你好！很高兴。",
                "¡Hola! Encantado.", "Hello! Nice to meet you.",
                "HSK1", "Desc", "Desc", LocalDate.now(), null);

        TextDTO saved = service.uploadText(data, null);

        assertNotNull(saved);
        assertEquals(2, saved.sentences().size());
        assertEquals("你好！", saved.sentences().get(0).chinese());
        assertEquals("Hello!", saved.sentences().get(0).english());
        assertEquals("¡Hola!", saved.sentences().get(0).spanish());
        assertEquals("很高兴。", saved.sentences().get(1).chinese());
        assertEquals("Nice to meet you.", saved.sentences().get(1).english());
        assertEquals("Encantado.", saved.sentences().get(1).spanish());
    }

    // Test unitario 10: uploadText rechaza un texto cuyas traducciones no cuadran
    // frase a frase con el chino (nunca se publica un texto desalineado)
    @Test
    @DisplayName("uploadText rejects mismatched sentence counts")
    public void testUploadTextRejectsMismatchedCounts() {
        TextService service = new TextService(textRepository, textMapper,
                new com.chinesereads.backend.Service.JiebaService(), dictionaryService, wordRepository);

        TextDTO data = new TextDTO(null, "Dialogue", "Diálogo", "你好！很高兴。",
                "¡Hola!", "Hello! Nice to meet you.",
                "HSK1", "Desc", "Desc", LocalDate.now(), null);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.uploadText(data, null));
        verify(textRepository, never()).save(any(Text.class));
    }

    // Test unitario 8: El lector público conserva los saltos como tokens "\n" alineados
    // 1:1 con las traducciones por palabra
    @Test
    @DisplayName("Reader endpoints keep line-break tokens aligned 1:1 with per-word translations")
    public void testGetTextSpanishKeepsBreakTokensAligned() {
        Text text = new Text("Title", "Título", "你好。\n再见。", "Hello.", "Hola.",
                "Description", "Descripción", "HSK1", LocalDate.now(), null);
        TextDTO dto = textMapper.toDTO(text);

        java.util.List<String> tokens = java.util.List.of("你好", "。", "\n", "再见", "。");
        when(jiebaService.segmentWithLayout("你好。\n再见。")).thenReturn(tokens);
        when(dictionaryService.translateToSpanish(tokens))
                .thenReturn(java.util.List.of("hola", "", "", "adiós", ""));

        String[][] result = textService.getTextSpanish(dto);

        assertEquals(5, result[0].length);
        assertEquals(result[0].length, result[1].length);
        assertEquals("\n", result[0][2]);
        assertEquals("", result[1][2]);
        assertEquals("再见", result[0][3]);
        assertEquals("adiós", result[1][3]);
    }
}