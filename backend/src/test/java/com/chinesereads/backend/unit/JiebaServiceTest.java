package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.chinesereads.backend.Service.JiebaService;

/**
 * Pins the layout-preserving segmentation shared by the public reader (Text)
 * and the private reader (UserText). Uses the real Jieba segmenter.
 */
public class JiebaServiceTest {

    private final JiebaService jiebaService = new JiebaService();

    @Test
    @DisplayName("A text without line breaks segments exactly like plain segment()")
    public void noBreaksBehavesLikePlainSegment() {
        String text = "我今天去学校。天气很好。";
        assertEquals(jiebaService.segment(text), jiebaService.segmentWithLayout(text));
    }

    @Test
    @DisplayName("A line break between sentences becomes one standalone token at a word boundary")
    public void breakBetweenSentencesBecomesToken() {
        List<String> tokens = jiebaService.segmentWithLayout("你好。\n再见。");

        // Exactly one break token, placed right after the token that closes the line.
        assertEquals(1, tokens.stream().filter(JiebaService.LINE_BREAK::equals).count());
        assertEquals("。", tokens.get(tokens.indexOf(JiebaService.LINE_BREAK) - 1));
        // Removing the break gives exactly the flat segmentation (words untouched).
        assertEquals(jiebaService.segment("你好。再见。"),
                tokens.stream().filter(t -> !JiebaService.LINE_BREAK.equals(t)).toList());
    }

    @Test
    @DisplayName("A cosmetic wrap inside a word never splits it (什\\n么 stays 什么)")
    public void midWordWrapKeepsWordWhole() {
        List<String> tokens = jiebaService.segmentWithLayout("什\n么");
        assertEquals(List.of("什么", JiebaService.LINE_BREAK), tokens);
    }

    @Test
    @DisplayName("Consecutive line breaks collapse into a single token")
    public void consecutiveBreaksCollapse() {
        List<String> tokens = jiebaService.segmentWithLayout("你好。\n\n再见。");
        assertEquals(1, tokens.stream().filter(JiebaService.LINE_BREAK::equals).count());
    }

    @Test
    @DisplayName("Leading line breaks are dropped")
    public void leadingBreaksAreDropped() {
        List<String> tokens = jiebaService.segmentWithLayout("\n你好");
        assertTrue(tokens.stream().noneMatch(JiebaService.LINE_BREAK::equals));
        assertEquals(jiebaService.segment("你好"), tokens);
    }
}
