package com.chinesereads.backend.dto;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.chinesereads.backend.Model.Flashcard;

@Mapper(componentModel = "spring")
public interface FlashcardMapper {

    @Mapping(target = "example", ignore = true)
    FlashcardDTO toDTO(Flashcard flashcard);

    List<FlashcardDTO> toDTO(List<Flashcard> flashcards);

    // The SRS fields are SM-2 state owned exclusively by the server (reviewFlashcard
    // computes them); the DTO deliberately never carries them, so a client can never
    // reset or forge its own review schedule.
    @Mapping(target = "example", ignore = true)
    @Mapping(target = "word", ignore = true)
    @Mapping(target = "collection", ignore = true)
    @Mapping(target = "srsRepetitions", ignore = true)
    @Mapping(target = "srsEase", ignore = true)
    @Mapping(target = "srsIntervalDays", ignore = true)
    @Mapping(target = "srsDueDate", ignore = true)
    Flashcard toDomain(FlashcardDTO flashcardDTO);
}