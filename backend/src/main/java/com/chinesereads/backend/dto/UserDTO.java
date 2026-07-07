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
    String newPassword,
    // GDPR: whether the user actively accepted the terms of use on signup. Sent by the
    // client and validated server-side. Defaults to false for callers that predate it.
    boolean termsAccepted){

    /** Backward-compatible constructor for existing call-sites that predate consent. */
    public UserDTO(Long id, String email, String name, String language,
            List<CollectionDTO> collections, List<String> roles, String password,
            String newPassword) {
        this(id, email, name, language, collections, roles, password, newPassword, false);
    }
}
