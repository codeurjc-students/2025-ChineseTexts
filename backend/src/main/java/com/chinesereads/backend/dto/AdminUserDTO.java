package com.chinesereads.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Compact user representation for the admin user list. Deliberately omits the
 * password hash and heavy relations — only what the admin table needs.
 */
public record AdminUserDTO(
        Long id,
        String email,
        String name,
        String language,
        List<String> roles,
        boolean blocked,
        LocalDate registrationDate,
        LocalDateTime lastAccess) {
}
