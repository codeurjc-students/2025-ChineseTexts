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
import com.chinesereads.backend.Model.UserTextWord;
import com.chinesereads.backend.Model.Word;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Repository.UserTextRepository;
import com.chinesereads.backend.Repository.WordRepository;
import com.chinesereads.backend.dto.UserTextReaderDTO;
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

        String chineseText = hasImage ? aiService.ocrImageToText(image) : pastedText.trim();
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

        // Title + full translations (best-effort; empty on AI failure).
        List<String> titles = aiService.getTitles(chineseText);
        String title = (titles != null && !titles.isEmpty() && titles.get(0) != null && !titles.get(0).isBlank())
                ? titles.get(0)
                : snippet(chineseText);
        userText.setTitle(title);

        List<String> translations = aiService.getTranslations(chineseText);
        userText.setEnglishTranslation(translations != null && translations.size() > 0 ? nz(translations.get(0)) : "");
        userText.setSpanishTranslation(translations != null && translations.size() > 1 ? nz(translations.get(1)) : "");

        // Per-word definitions: reuse the global dictionary where possible, ask the AI
        // only for the rest — and store them ONLY on this text (never in the global dict).
        List<String> segments = jiebaService.segment(chineseText);
        Map<String, String[]> defs = resolveDefinitions(segments);

        int position = 0;
        for (String seg : segments) {
            String[] d = defs.getOrDefault(seg, EMPTY_DEF);
            userText.addWord(new UserTextWord(position++, seg, d[0], d[1], d[2]));
        }

        UserText saved = userTextRepository.save(userText);
        return toSummary(saved);
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
                .map(w -> new UserTextWordDTO(w.getChinese(), w.getPinyin(), w.getEnglish(), w.getSpanish()))
                .toList();
        return new UserTextReaderDTO(
                text.getId(),
                text.getTitle(),
                text.getText(),
                text.getEnglishTranslation(),
                text.getSpanishTranslation(),
                text.getCreationDate(),
                words);
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

    /** Builds a map chinese -> [pinyin, english, spanish] for the unique segments. */
    private Map<String, String[]> resolveDefinitions(List<String> segments) {
        Map<String, String[]> defs = new HashMap<>();
        List<String> missing = new ArrayList<>();

        for (String seg : segments.stream().distinct().toList()) {
            if (seg.isBlank()) {
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

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String snippet(String chineseText) {
        String clean = chineseText.strip();
        return clean.length() > 20 ? clean.substring(0, 20) + "…" : clean;
    }
}
