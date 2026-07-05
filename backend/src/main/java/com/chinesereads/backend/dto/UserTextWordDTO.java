package com.chinesereads.backend.dto;

/** One segment of a user's private text with its pinyin and translations. */
public record UserTextWordDTO(
        String chinese,
        String pinyin,
        String english,
        String spanish) {
}
