package com.chinesereads.backend.Service;

import java.sql.SQLException;
import java.util.List;

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
}
