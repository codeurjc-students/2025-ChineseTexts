package com.chinesereads.backend.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.chinesereads.backend.Model.Text;
import com.chinesereads.backend.Repository.TextRepository;
import com.chinesereads.backend.dto.TextDTO;
import com.chinesereads.backend.dto.TextMapper;

@Service
public class TextService {

    @Autowired
    private TextRepository textRepository;

    @Autowired
    private TextMapper textMapper;

    @Autowired
    private JiebaService jiebaService;

    @Autowired
    private DictionaryService dictionaryService;

    // Guardar texto (solo para inicializar datos)
    public TextDTO save(Text text) {
        if (textRepository.findByTitleEnglish(text.getTitleEnglish()).isPresent()
                || textRepository.findByTitleSpanish(text.getTitleSpanish()).isPresent()) {
            return null;
        } else {
            return textMapper.toDTO(textRepository.save(text));
        }
    }

    // Obtener textos sin filtro (paginados)
    public List<TextDTO> getTexts(int page, int size) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("creationDate").descending().and(Sort.by("id").descending())
        );

        Page<Text> result = textRepository.findAll(pageable);
        return textMapper.toDTO(result.getContent());
    }

    // Obtener textos filtrados por nivel (paginados)
    public List<TextDTO> getTextsByLevel(String level, int page, int size) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("creationDate").descending().and(Sort.by("id").descending())
        );

        Page<Text> result = textRepository.findByLevel(level, pageable);
        return textMapper.toDTO(result.getContent());
    }

    // Obtener imagen del texto
    public Resource getTextImage(Long textId) {
        Text text = textRepository.findById(textId).orElseThrow();

        try {
            return new InputStreamResource(text.getImage().getBinaryStream());
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving text image", e);
        }
    }

    public TextDTO getText(long id){
        Optional<Text> text = this.textRepository.findById(id);
        if(text.isPresent()){
            return textMapper.toDTO(text.get());
        } else {
            return null;
        }
    }

    public String[][] getTextSpanish(TextDTO text){
        List<String> textSegmented = jiebaService.segment(text.text());
        List<String> words = dictionaryService.translateToSpanish(textSegmented);

        String[] chineseArray = textSegmented.toArray(new String[0]);
        String[] spanishArray = words.toArray(new String[0]);

        String[][] result = new String[2][];
        result[0] = chineseArray;
        result[1] = spanishArray;
        return result;
    }

    public String[][] getTextEnglish(TextDTO text){
        List<String> textSegmented = jiebaService.segment(text.text());
        List<String> words = dictionaryService.translateToEnglish(textSegmented);

        String[] chineseArray = textSegmented.toArray(new String[0]);
        String[] englishArray = words.toArray(new String[0]);

        String[][] result = new String[2][];
        result[0] = chineseArray;
        result[1] = englishArray;
        return result;
    }
}
