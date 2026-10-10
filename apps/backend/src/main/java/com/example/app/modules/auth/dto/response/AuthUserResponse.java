package com.example.app.modules.auth.dto.response;

import com.example.app.common.security.Role;

/** The authenticated user as the clients need it (no status/audit data). */
public record AuthUserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        Role role) {
}
