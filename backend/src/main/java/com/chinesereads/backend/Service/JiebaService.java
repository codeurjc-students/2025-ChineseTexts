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

    public List<String> segment(String text){
        JiebaSegmenter segmenter = new JiebaSegmenter();
        List<SegToken> tokens = segmenter.process(text, JiebaSegmenter.SegMode.SEARCH);
        return tokens.stream().map(token -> token.word).collect(Collectors.toList());
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