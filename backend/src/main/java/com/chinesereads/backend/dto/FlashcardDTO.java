package com.chinesereads.backend.dto;

public record FlashcardDTO(
    Long id,
    WordDTO word,
    TextDTO example,
    CollectionDTO collection) {
}
