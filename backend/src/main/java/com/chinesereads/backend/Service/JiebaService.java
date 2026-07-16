package com.chinesereads.backend.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;

@Service
public class JiebaService {         //Segmenta el texto en palabras

    /** Token emitted by {@link #segmentWithLayout} to mark a line break between words. */
    public static final String LINE_BREAK = "\n";

    /**
     * Sentence-final terminators, shared by every sentence split in the app
     * (private texts, public texts and — mirrored — the frontend readers).
     */
    public static final String SENTENCE_TERMINATORS = "。！？…；.!?;";

    public List<String> segment(String text){
        JiebaSegmenter segmenter = new JiebaSegmenter();
        List<SegToken> tokens = segmenter.process(text, JiebaSegmenter.SegMode.SEARCH);
        return tokens.stream().map(token -> token.word).collect(Collectors.toList());
    }

    /** True when the token's last char is a sentence terminator. */
    public static boolean endsWithTerminator(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        return SENTENCE_TERMINATORS.indexOf(token.charAt(token.length() - 1)) >= 0;
    }

    /**
     * Groups ordered segments into sentences, ending ONLY on a terminator
     * (。！？…；.!?;). A trailing group without terminator still forms a final
     * sentence. Shared by the private flow (UserTextService) and the public
     * flow (TextService/AiService) so all sentence boundaries agree.
     */
    public List<String> buildSentences(List<String> segments) {
        List<String> sentences = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String seg : segments) {
            current.append(seg);
            if (endsWithTerminator(seg)) {
                String s = current.toString().strip();
                if (!s.isEmpty()) {
                    sentences.add(s);
                }
                current.setLength(0);
            }
        }
        String tail = current.toString().strip();
        if (!tail.isEmpty()) {
            sentences.add(tail);
        }
        return sentences;
    }

    /**
     * Splits a TRANSLATED block (English/Spanish) into sentences: after a
     * terminator followed by whitespace or end of text. Mirror of the
     * frontend's splitTranslatedSentences (utils/sentence.util.ts) — both must
     * stay in sync so counts agree end to end.
     */
    public List<String> splitTranslatedBlock(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(text.split("(?<=[。！？…；.!?;])(?=\\s|$)"))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Segments text that may contain line breaks (photographed dialogues,
     * conversations) keeping the layout: the WHOLE text is segmented at once —
     * never line by line — so a word split by a cosmetic line wrap in the photo
     * (e.g. 什\n么) stays one word, and each line break is then snapped to the
     * nearest word boundary and emitted as a standalone {@link #LINE_BREAK}
     * token after that word — never inside one. Consecutive breaks collapse to
     * one and leading breaks are dropped, so a text without line breaks returns
     * exactly the same list as {@link #segment}.
     */
    public List<String> segmentWithLayout(String text) {
        // Strip the breaks, remembering the stripped char index each one falls after.
        StringBuilder strippedBuilder = new StringBuilder();
        List<Integer> breakAfterChar = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                if (strippedBuilder.length() > 0) {
                    breakAfterChar.add(strippedBuilder.length() - 1);
                }
            } else {
                strippedBuilder.append(c);
            }
        }
        String stripped = strippedBuilder.toString();

        List<String> result = new ArrayList<>();
        int cursor = 0;   // running char offset within the stripped text
        int brIdx = 0;    // pointer into breakAfterChar
        for (String seg : segment(stripped)) {
            if (seg.isEmpty()) {
                continue;
            }
            int start = stripped.indexOf(seg, cursor);
            if (start < 0) {
                start = cursor;
            }
            int end = start + seg.length();
            result.add(seg);
            // Snap every break that falls within this token onto its boundary; a run
            // of breaks collapses into the single LINE_BREAK emitted after the word.
            boolean breakHere = false;
            while (brIdx < breakAfterChar.size() && breakAfterChar.get(brIdx) < end) {
                breakHere = true;
                brIdx++;
            }
            if (breakHere) {
                result.add(LINE_BREAK);
            }
            cursor = end;
        }
        return result;
    }

}