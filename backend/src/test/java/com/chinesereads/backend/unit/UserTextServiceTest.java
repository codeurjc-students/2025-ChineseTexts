package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.chinesereads.backend.Repository.WordRepository;
import com.chinesereads.backend.Service.AiService;
import com.chinesereads.backend.Service.UserTextService;

/**
 * Focused tests for the sentence-grouping logic that keeps a user's private text
 * aligned with its per-sentence translations. Exercises the private buildSentences
 * via reflection (same style as {@link UsageServiceTest}'s field injection).
 */
public class UserTextServiceTest {

    @SuppressWarnings("unchecked")
    private List<String> buildSentences(List<String> segments) {
        try {
            Method m = UserTextService.class.getDeclaredMethod("buildSentences", List.class);
            m.setAccessible(true);
            return (List<String>) m.invoke(new UserTextService(null, null, null, null, null, null), segments);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Splits on Chinese terminators (。？！)")
    public void testSplitsOnTerminators() {
        List<String> segments = List.of("你好", "。", "你", "好吗", "？");

        List<String> sentences = buildSentences(segments);

        assertEquals(List.of("你好。", "你好吗？"), sentences);
    }

    @Test
    @DisplayName("A line break without a terminator does NOT split (avoids mid-word/mid-sentence cuts)")
    public void testDoesNotSplitOnLineWrap() {
        // A sentence wrapped across two photo lines with no punctuation at the wrap:
        // 你做什 | 么运动了？  must stay as ONE sentence.
        List<String> segments = List.of("你", "做", "什么", "运动", "了", "？");

        List<String> sentences = buildSentences(segments);

        assertEquals(List.of("你做什么运动了？"), sentences);
    }

    @Test
    @DisplayName("Trailing text without a terminator becomes its own sentence")
    public void testTrailingWithoutTerminator() {
        List<String> segments = List.of("第一句", "。", "没有句号的结尾");

        List<String> sentences = buildSentences(segments);

        assertEquals(List.of("第一句。", "没有句号的结尾"), sentences);
    }

    @Test
    @DisplayName("Empty input yields no sentences")
    public void testEmpty() {
        assertEquals(List.of(), buildSentences(List.of()));
    }

    @Test
    @DisplayName("Words the AI omits from a batch are retried so no character is left without a definition")
    @SuppressWarnings("unchecked")
    public void testDefinitionRetryFillsGaps() throws Exception {
        WordRepository wordRepository = mock(WordRepository.class);
        AiService aiService = mock(AiService.class);
        // Nothing is in the global dictionary, so both words go to the AI.
        when(wordRepository.findByChinese(anyString())).thenReturn(Optional.empty());
        // First batch omits 世界; the retry returns it.
        when(aiService.getWordDefinitions(any()))
            .thenReturn(List.of(Map.of("chinese", "你好", "pinyin", "nǐ hǎo", "english", "hello", "spanish", "hola")))
            .thenReturn(List.of(Map.of("chinese", "世界", "pinyin", "shì jiè", "english", "world", "spanish", "mundo")));

        UserTextService service = new UserTextService(null, null, wordRepository, null, aiService, null);

        Method m = UserTextService.class.getDeclaredMethod("resolveDefinitions", List.class);
        m.setAccessible(true);
        Map<String, String[]> defs = (Map<String, String[]>) m.invoke(service, List.of("你好", "世界"));

        assertEquals("hello", defs.get("你好")[1]);
        assertEquals("world", defs.get("世界")[1]); // filled by the retry
        verify(aiService, times(2)).getWordDefinitions(any());
    }
}
