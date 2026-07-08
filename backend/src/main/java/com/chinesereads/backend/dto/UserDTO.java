package com.chinesereads.backend.dto;

import java.time.LocalDateTime;
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
    boolean termsAccepted,
    // Read-only: when the PREMIUM subscription expires (null = free plan). Surfaced on
    // /api/users/me so the client can show the user's plan; never set from the client.
    LocalDateTime premiumUntil){

    /** Backward-compatible constructor for existing call-sites that predate consent. */
    public UserDTO(Long id, String email, String name, String language,
            List<CollectionDTO> collections, List<String> roles, String password,
            String newPassword) {
        this(id, email, name, language, collections, roles, password, newPassword, false, null);
    }

    /** Backward-compatible constructor for call-sites that predate the premium field. */
    public UserDTO(Long id, String email, String name, String language,
            List<CollectionDTO> collections, List<String> roles, String password,
            String newPassword, boolean termsAccepted) {
        this(id, email, name, language, collections, roles, password, newPassword, termsAccepted, null);
    }
}
