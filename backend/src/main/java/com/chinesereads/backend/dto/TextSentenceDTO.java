package com.chinesereads.backend.dto;

/** One aligned sentence of a public text: chinese ↔ english ↔ spanish. */
public record TextSentenceDTO(
    String chinese,
    String english,
    String spanish) {
}
