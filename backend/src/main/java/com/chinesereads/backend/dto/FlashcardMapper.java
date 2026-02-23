package com.chinesereads.backend.dto;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.chinesereads.backend.Model.Flashcard;

@Mapper(componentModel = "spring")
public interface FlashcardMapper {

    @Mapping(target = "word", ignore = true)
    FlashcardDTO toDTO(Flashcard flashcard);

    List<FlashcardDTO> toDTO(List<Flashcard> flashcards);

    @Mapping(target = "example", ignore = true)
    Flashcard toDomain(FlashcardDTO flashcardDTO);
}
