package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
            return (List<String>) m.invoke(new UserTextService(), segments);
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
}
