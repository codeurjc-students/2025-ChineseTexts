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

    // ——— buildSentences (shared by private AND public texts) ———

    @Test
    @DisplayName("buildSentences closes on every terminator (。！？) and keeps a tail without one")
    public void buildSentencesClosesOnTerminatorsAndKeepsTail() {
        assertEquals(List.of("你好。", "你好吗？"),
                jiebaService.buildSentences(List.of("你好", "。", "你", "好吗", "？")));
        assertEquals(List.of("第一句。", "没有句号的结尾"),
                jiebaService.buildSentences(List.of("第一句", "。", "没有句号的结尾")));
        assertEquals(List.of("你好！", "很高兴。"),
                jiebaService.buildSentences(List.of("你好", "！", "很", "高兴", "。")));
    }

    // ——— splitTranslatedBlock (mirror of the frontend's splitTranslatedSentences) ———

    @Test
    @DisplayName("splitTranslatedBlock splits after terminator + space/end, keeping each terminator")
    public void splitTranslatedBlockSplitsLikeTheFrontend() {
        assertEquals(List.of("Hello!", "Nice to meet you.", "Goodbye."),
                jiebaService.splitTranslatedBlock("Hello! Nice to meet you. Goodbye."));
        // A terminator NOT followed by whitespace/end does not split (e.g. "3.5").
        assertEquals(List.of("It costs 3.5 yuan."),
                jiebaService.splitTranslatedBlock("It costs 3.5 yuan."));
        assertEquals(List.of(), jiebaService.splitTranslatedBlock("   "));
        assertEquals(List.of(), jiebaService.splitTranslatedBlock(null));
    }
}
