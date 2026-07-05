package com.chinesereads.backend.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Model.UserText;
import com.chinesereads.backend.Model.UserTextSentence;
import com.chinesereads.backend.Model.UserTextWord;
import com.chinesereads.backend.Model.Word;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Repository.UserTextRepository;
import com.chinesereads.backend.Repository.WordRepository;
import com.chinesereads.backend.dto.UserTextReaderDTO;
import com.chinesereads.backend.dto.UserTextSentenceDTO;
import com.chinesereads.backend.dto.UserTextSummaryDTO;
import com.chinesereads.backend.dto.UserTextWordDTO;

/**
 * Creates and serves users' PRIVATE texts. Each text is processed once (OCR or
 * pasted → segmented → per-word pinyin/translations resolved) and stored fully
 * self-contained, so word definitions stay private and never touch the global
 * dictionary. All reads/deletes are owner-scoped.
 */
@Service
public class UserTextService {

    @Autowired
    private UserTextRepository userTextRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private JiebaService jiebaService;

    @Autowired
    private AiService aiService;

    @Autowired
    private UsageService usageService;

    /** Max characters per text: bounds per-generation API cost and keeps full-text audio working. */
    @Value("${usage.text.max-chars:1500}")
    private int maxChars;

    /**
     * Builds a private text from an uploaded image (OCR) or pasted Chinese text.
     * Reserves usage BEFORE any paid API call so cost stays bounded.
     */
    @Transactional
    public UserTextSummaryDTO create(String ownerEmail, String pastedText, MultipartFile image) throws Exception {
        boolean hasImage = image != null && !image.isEmpty();
        boolean hasPasted = pastedText != null && !pastedText.isBlank();
        if (!hasImage && !hasPasted) {
            throw new IllegalArgumentException("Provide an image or some text.");
        }

        User owner = userRepository.findByEmail(ownerEmail).orElseThrow();

        // Cost guard: consumes one unit from the per-user and global counters, or throws.
        usageService.reserveGeneration(owner);

        String chineseText = hasImage ? aiService.ocrImageToText(image, true) : pastedText.trim();
        if (chineseText == null || chineseText.isBlank()) {
            throw new IllegalArgumentException("No Chinese text could be read from the image.");
        }
        chineseText = chineseText.trim();
        if (chineseText.length() > maxChars) {
            throw new IllegalArgumentException(
                    "This text is too long (max " + maxChars + " characters). Please split it into shorter pieces.");
        }

        UserText userText = new UserText();
        userText.setOwner(owner);
        userText.setText(chineseText);
        userText.setCreationDate(LocalDate.now());

        // Title (best-effort; empty on AI failure).
        List<String> titles = aiService.getTitles(chineseText);
        String title = (titles != null && !titles.isEmpty() && titles.get(0) != null && !titles.get(0).isBlank())
                ? titles.get(0)
                : snippet(chineseText);
        userText.setTitle(title);

        // Segment line by line so the original layout (dialogues, line breaks) is
        // preserved: a newline flag is set on the last word of each line.
        List<String> segments = new ArrayList<>();
        List<Boolean> newlineFlags = new ArrayList<>();
        String[] lines = chineseText.split("\n", -1);
        for (int li = 0; li < lines.length; li++) {
            for (String seg : jiebaService.segment(lines[li])) {
                if (!seg.isEmpty()) {
                    segments.add(seg);
                    newlineFlags.add(false);
                }
            }
            if (li < lines.length - 1 && !newlineFlags.isEmpty()) {
                newlineFlags.set(newlineFlags.size() - 1, true);
            }
        }

        // Group into sentences (ending on a terminator OR a line break) and translate
        // each one on its own, so the Chinese and its translation stay aligned 1:1.
        // The full translations are then just the sentences joined, so both views agree.
        List<String> sentenceTexts = buildSentences(segments, newlineFlags);
        List<List<String>> sentencePairs = aiService.getSentenceTranslations(sentenceTexts);

        List<String> englishParts = new ArrayList<>();
        List<String> spanishParts = new ArrayList<>();
        for (int i = 0; i < sentenceTexts.size(); i++) {
            List<String> pair = (sentencePairs != null && i < sentencePairs.size()) ? sentencePairs.get(i) : null;
            String en = (pair != null && pair.size() > 0) ? nz(pair.get(0)) : "";
            String es = (pair != null && pair.size() > 1) ? nz(pair.get(1)) : "";
            userText.addSentence(new UserTextSentence(i, sentenceTexts.get(i), en, es));
            if (!en.isBlank()) englishParts.add(en);
            if (!es.isBlank()) spanishParts.add(es);
        }
        userText.setEnglishTranslation(String.join(" ", englishParts));
        userText.setSpanishTranslation(String.join(" ", spanishParts));

        // Per-word definitions: reuse the global dictionary where possible, ask the AI
        // only for the rest — and store them ONLY on this text (never in the global dict).
        // Punctuation / non-Chinese segments are never sent to the AI (no fake pinyin).
        Map<String, String[]> defs = resolveDefinitions(segments);

        for (int i = 0; i < segments.size(); i++) {
            String seg = segments.get(i);
            String[] d = defs.getOrDefault(seg, EMPTY_DEF);
            UserTextWord word = new UserTextWord(i, seg, d[0], d[1], d[2]);
            word.setNewlineAfter(newlineFlags.get(i));
            userText.addWord(word);
        }

        UserText saved = userTextRepository.save(userText);
        return toSummary(saved);
    }

    /**
     * OCR "extract" step: reads an image and returns the extracted Chinese text with
     * its original layout, for the user to review/edit before generating. Cheaper than
     * a full generation, so it only consumes from the global daily counter.
     */
    @Transactional
    public String extract(String ownerEmail, MultipartFile image) throws Exception {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Please choose an image.");
        }
        userRepository.findByEmail(ownerEmail).orElseThrow();
        usageService.reserveOcr();

        String text = aiService.ocrImageToText(image, true);
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("No Chinese text could be read from the image.");
        }
        return text.strip();
    }

    @Transactional(readOnly = true)
    public List<UserTextSummaryDTO> listMine(String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail).orElseThrow();
        return userTextRepository.findByOwnerOrderByCreationDateDescIdDesc(owner).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserTextReaderDTO getMine(long id, String ownerEmail) {
        UserText text = ownedOrThrow(id, ownerEmail);
        List<UserTextWordDTO> words = text.getWords().stream()
                .map(w -> new UserTextWordDTO(w.getChinese(), w.getPinyin(), w.getEnglish(),
                        w.getSpanish(), w.isNewlineAfter()))
                .toList();
        List<UserTextSentenceDTO> sentences = text.getSentences().stream()
                .map(s -> new UserTextSentenceDTO(s.getChinese(), s.getEnglish(), s.getSpanish()))
                .toList();
        return new UserTextReaderDTO(
                text.getId(),
                text.getTitle(),
                text.getText(),
                text.getEnglishTranslation(),
                text.getSpanishTranslation(),
                text.getCreationDate(),
                words,
                sentences);
    }

    @Transactional
    public void deleteMine(long id, String ownerEmail) {
        UserText text = ownedOrThrow(id, ownerEmail);
        userTextRepository.delete(text);
    }

    // ——————————————————————————— Helpers ———————————————————————————

    private static final String[] EMPTY_DEF = { "", "", "" };

    /**
     * Loads a text only if it belongs to the caller; otherwise throws
     * NoSuchElementException so other users' texts are indistinguishable from
     * non-existent ones (404, never leaking their existence).
     */
    private UserText ownedOrThrow(long id, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail).orElseThrow();
        return userTextRepository.findById(id)
                .filter(t -> t.getOwner() != null && t.getOwner().getId() == owner.getId())
                .orElseThrow(() -> new NoSuchElementException("Text not found"));
    }

    /** Chinese sentence terminators used to split a text into sentences. */
    private static final String SENTENCE_TERMINATORS = "。！？…；.!?;";

    /**
     * Groups the ordered segments into sentences. A sentence ends when a segment
     * finishes with a terminator (。！？…；．！？) OR when a line break follows it, so
     * dialogues and lines without punctuation are still split correctly (and stay
     * aligned with the layout shown word by word).
     */
    private List<String> buildSentences(List<String> segments, List<Boolean> newlineFlags) {
        List<String> sentences = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            current.append(segments.get(i));
            boolean endsHere = endsWithTerminator(segments.get(i)) || Boolean.TRUE.equals(newlineFlags.get(i));
            if (endsHere) {
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

    private static boolean endsWithTerminator(String seg) {
        if (seg == null || seg.isEmpty()) {
            return false;
        }
        return SENTENCE_TERMINATORS.indexOf(seg.charAt(seg.length() - 1)) >= 0;
    }

    /** Builds a map chinese -> [pinyin, english, spanish] for the unique segments. */
    private Map<String, String[]> resolveDefinitions(List<String> segments) {
        Map<String, String[]> defs = new HashMap<>();
        List<String> missing = new ArrayList<>();

        for (String seg : segments.stream().distinct().toList()) {
            // Skip punctuation / non-Chinese segments: they render as-is with no
            // pinyin/translation and are never sent to the AI (avoids fake pinyin).
            if (seg.isBlank() || !containsCjk(seg)) {
                continue;
            }
            Word w = wordRepository.findByChinese(seg).orElse(null);
            if (w != null) {
                defs.put(seg, new String[] { nz(w.getPinyin()), nz(w.getEnglish()), nz(w.getSpanish()) });
            } else {
                missing.add(seg);
            }
        }

        for (Map<String, String> m : aiService.getWordDefinitions(missing)) {
            String chinese = m.get("chinese");
            if (chinese != null && !chinese.isBlank()) {
                defs.put(chinese, new String[] { nz(m.get("pinyin")), nz(m.get("english")), nz(m.get("spanish")) });
            }
        }
        return defs;
    }

    private UserTextSummaryDTO toSummary(UserText t) {
        return new UserTextSummaryDTO(t.getId(), t.getTitle(), t.getCreationDate(),
                t.getWords() != null ? t.getWords().size() : 0);
    }

    /** True if the segment contains at least one CJK ideograph (i.e. is a real word). */
    private static boolean containsCjk(String s) {
        return s.codePoints().anyMatch(cp ->
                (cp >= 0x4E00 && cp <= 0x9FFF) || (cp >= 0x3400 && cp <= 0x4DBF));
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String snippet(String chineseText) {
        String clean = chineseText.strip();
        return clean.length() > 20 ? clean.substring(0, 20) + "…" : clean;
    }
}
