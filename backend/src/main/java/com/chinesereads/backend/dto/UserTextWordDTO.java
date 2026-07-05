package com.chinesereads.backend.dto;

/** One segment of a user's private text with its pinyin, translations and layout. */
public record UserTextWordDTO(
        String chinese,
        String pinyin,
        String english,
        String spanish,
        boolean newlineAfter) {
}
