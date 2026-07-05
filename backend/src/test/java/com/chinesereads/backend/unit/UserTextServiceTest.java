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
    private List<String> buildSentences(List<String> segments, List<Boolean> newlineFlags) {
        try {
            Method m = UserTextService.class.getDeclaredMethod("buildSentences", List.class, List.class);
            m.setAccessible(true);
            return (List<String>) m.invoke(new UserTextService(), segments, newlineFlags);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Splits on Chinese terminators (。？！)")
    public void testSplitsOnTerminators() {
        List<String> segments = List.of("你好", "。", "你", "好吗", "？");
        List<Boolean> flags = List.of(false, false, false, false, false);

        List<String> sentences = buildSentences(segments, flags);

        assertEquals(List.of("你好。", "你好吗？"), sentences);
    }

    @Test
    @DisplayName("Splits on a line break even without a terminator (dialogue lines)")
    public void testSplitsOnLineBreak() {
        // Two dialogue lines with no punctuation; the newline flag ends the first.
        List<String> segments = List.of("我", "来", "你", "去");
        List<Boolean> flags = List.of(false, true, false, false);

        List<String> sentences = buildSentences(segments, flags);

        assertEquals(List.of("我来", "你去"), sentences);
    }

    @Test
    @DisplayName("Trailing text without a terminator becomes its own sentence")
    public void testTrailingWithoutTerminator() {
        List<String> segments = List.of("第一句", "。", "没有句号的结尾");
        List<Boolean> flags = List.of(false, false, false);

        List<String> sentences = buildSentences(segments, flags);

        assertEquals(List.of("第一句。", "没有句号的结尾"), sentences);
    }

    @Test
    @DisplayName("Empty input yields no sentences")
    public void testEmpty() {
        assertEquals(List.of(), buildSentences(List.of(), List.of()));
    }
}
