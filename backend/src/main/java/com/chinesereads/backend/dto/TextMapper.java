package com.chinesereads.backend.dto;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.chinesereads.backend.Model.Text;

@Mapper(componentModel = "spring")
public interface TextMapper {

    TextDTO toDTO(Text text);

    List<TextDTO> toDTO(List<Text> texts);

    @Mapping(target = "image", ignore = true)
    // Sentences are built + validated by TextService.uploadText itself — never
    // taken raw from a client body.
    @Mapping(target = "sentences", ignore = true)
    Text toDomain(TextDTO textDTO);

    // Element mapping for Text.sentences -> TextDTO.sentences, declared here
    // (not via uses=) so the generated impl stays dependency-free and unit
    // tests can keep instantiating it with plain `new`.
    TextSentenceDTO sentenceToDTO(com.chinesereads.backend.Model.TextSentence sentence);
}
