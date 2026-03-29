package com.chinesereads.backend.dto;

import java.util.List;

public record ValidationResultDTO(
    boolean valid,
    List<String> missingWords,
    List<String> segments
) {}