package com.chinesereads.backend.dto;

import java.util.List;

public record UserDTO (
    Long id,
    String email,
    String name,
    String language,
    List<CollectionDTO> collections,
    List<String> roles,
    String password,
    String newPassword){
}
