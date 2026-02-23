package com.chinesereads.backend.dto;

import java.time.LocalDate;

public record CollectionDTO(
    Long id,
    String title,
    LocalDate date) {
}
