package com.chinesereads.backend.dto;

public record WordDTO (
    Long id,
    String pinyin,
    String chinese,
    String english,
    String spanish){
}
