package com.chinesereads.backend.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.engine.jdbc.proxy.BlobProxy;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.chinesereads.backend.Model.Text;
import com.chinesereads.backend.Repository.TextRepository;
import com.chinesereads.backend.Repository.WordRepository;
import com.chinesereads.backend.dto.TextDTO;
import com.chinesereads.backend.dto.TextMapper;
import com.chinesereads.backend.dto.ValidationResultDTO;

@Service
public class TextService {

    private final TextRepository textRepository;

    private final TextMapper textMapper;

    private final JiebaService jiebaService;

    private final DictionaryService dictionaryService;

    private final WordRepository wordRepository;

    public TextService(TextRepository textRepository, TextMapper textMapper, JiebaService jiebaService, DictionaryService dictionaryService, WordRepository wordRepository) {
        this.textRepository = textRepository;
        this.textMapper = textMapper;
        this.jiebaService = jiebaService;
        this.dictionaryService = dictionaryService;
        this.wordRepository = wordRepository;
    }

    public TextDTO save(Text text) {
        if (textRepository.findByTitleEnglish(text.getTitleEnglish()).isPresent()
                || textRepository.findByTitleSpanish(text.getTitleSpanish()).isPresent()) {
            return null;
        } else {
            return textMapper.toDTO(textRepository.save(text));
        }
    }

    /** Id + creation date of every text, for the dynamic sitemap (/sitemap-texts.xml). */
    public List<TextRepository.TextSitemapRow> getSitemapRows() {
        return textRepository.findSitemapRows();
    }

    public List<TextDTO> getTexts(int page, int size) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("creationDate").descending().and(Sort.by("id").descending())
        );
        Page<Text> result = textRepository.findAll(pageable);
        return textMapper.toDTO(result.getContent());
    }

    public List<TextDTO> getTextsByLevel(String level, int page, int size) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("creationDate").descending().and(Sort.by("id").descending())
        );
        Page<Text> result = textRepository.findByLevel(level, pageable);
        return textMapper.toDTO(result.getContent());
    }

    public Resource getTextImage(Long textId) {
        Text text = textRepository.findById(textId).orElseThrow();
        try {
            return new InputStreamResource(text.getImage().getBinaryStream());
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving text image", e);
        }
    }

    public TextDTO getText(long id) {
        Optional<Text> text = this.textRepository.findById(id);
        return text.map(textMapper::toDTO).orElse(null);
    }

    // The reader endpoints segment preserving the layout: texts stored with line
    // breaks (photographed dialogues) emit standalone "\n" tokens the frontend
    // renders as visual breaks. Both arrays stay aligned 1:1 (a break maps to "").
    // Texts without breaks — all pre-existing ones — segment exactly as before.
    public String[][] getTextSpanish(TextDTO text) {
        List<String> textSegmented = jiebaService.segmentWithLayout(text.text());
        List<String> words = dictionaryService.translateToSpanish(textSegmented);
        return new String[][] {
            textSegmented.toArray(new String[0]),
            words.toArray(new String[0])
        };
    }

    public String[][] getTextEnglish(TextDTO text) {
        List<String> textSegmented = jiebaService.segmentWithLayout(text.text());
        List<String> words = dictionaryService.translateToEnglish(textSegmented);
        return new String[][] {
            textSegmented.toArray(new String[0]),
            words.toArray(new String[0])
        };
    }

    public ValidationResultDTO validateText(String chineseText) {
        // Dictionary validation ignores layout: breaks are stripped BEFORE
        // segmenting so a word split by a line wrap (什\n么) is still one word.
        List<String> segments = jiebaService.segment(chineseText.replace("\n", ""));
        List<String> missing = segments.stream()
                .distinct()
                .filter(word -> !word.isBlank())
                .filter(word -> wordRepository.findByChinese(word).isEmpty())
                .collect(Collectors.toList());
        return new ValidationResultDTO(missing.isEmpty(), missing, segments);
    }

    public TextDTO uploadText(TextDTO data, MultipartFile image) throws IOException {
        Text text = new Text(
                data.titleEnglish(),
                data.titleSpanish(),
                data.text(),
                data.englishTranslation(),
                data.spanishTranslation(),
                data.englishDescription(),
                data.spanishDescription(),
                data.level(),
                data.creationDate() != null ? data.creationDate() : LocalDate.now(),
                null
        );
        buildAlignedSentences(text);
        if (image != null && !image.isEmpty()) {
            text.setImage(BlobProxy.generateProxy(image.getInputStream(), image.getSize()));
        }
        return save(text);
    }

    /**
     * Builds the aligned sentence pairs for a NEW public text and validates them:
     * the Chinese sentences (shared segmentation + terminators) must match the
     * sentence count of BOTH submitted translations. This is what guarantees the
     * reader never has to guess the pairing again — any misaligned text is
     * rejected at the door with a clear message instead of being published broken.
     */
    private void buildAlignedSentences(Text text) {
        List<String> chineseSentences = jiebaService.buildSentences(
                jiebaService.segment(text.getText().replace("\n", "")));
        List<String> english = jiebaService.splitTranslatedBlock(text.getEnglishTranslation());
        List<String> spanish = jiebaService.splitTranslatedBlock(text.getSpanishTranslation());

        if (chineseSentences.isEmpty()
                || english.size() != chineseSentences.size()
                || spanish.size() != chineseSentences.size()) {
            throw new IllegalArgumentException(
                    "Sentence counts do not match: " + chineseSentences.size() + " Chinese, "
                    + english.size() + " English, " + spanish.size() + " Spanish. "
                    + "Each Chinese sentence needs exactly one translated sentence.");
        }

        for (int i = 0; i < chineseSentences.size(); i++) {
            text.addSentence(new com.chinesereads.backend.Model.TextSentence(
                    i, chineseSentences.get(i), english.get(i), spanish.get(i)));
        }
    }

    public void deleteText(long id) {
        if (!textRepository.existsById(id)) {
            throw new RuntimeException("Text not found with id: " + id);
        }
        textRepository.deleteById(id);
    }
}