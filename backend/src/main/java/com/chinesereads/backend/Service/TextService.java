package com.chinesereads.backend.Service;

import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
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
    
    public TextDTO save(Text text){     //Este metodo es solo para inicializar los datos (toma Text directamente)
        if(textRepository.findByTitleEnglish(text.getTitleEnglish()).isPresent() || textRepository.findByTitleSpanish(text.getTitleSpanish()).isPresent()){
            return null;
        } else{
            return textMapper.toDTO(textRepository.save(text));
        }
    }

    public List<TextDTO> getTexts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("creationDate").descending().and(Sort.by("id").descending()));
        return textMapper.toDTO(textRepository.findAll(pageable).getContent());
    }

    public Resource getTextImage(Long textId) {
        Text text = textRepository.findById(textId).orElseThrow();
        try {
            return new InputStreamResource(text.getImage().getBinaryStream());
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving user image", e);
        }
    }

}