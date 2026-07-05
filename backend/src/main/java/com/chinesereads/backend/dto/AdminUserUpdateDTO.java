package com.chinesereads.backend.dto;

import java.util.List;

/**
 * Fields an admin may edit on another user from the management panel:
 * display name, preferred language and roles (e.g. granting/revoking ADMIN).
 * Any null field is left unchanged.
 */
public record AdminUserUpdateDTO(
        String name,
        String language,
        List<String> roles) {
}
