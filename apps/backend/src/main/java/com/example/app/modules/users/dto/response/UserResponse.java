package com.example.app.modules.users.dto.response;

import com.example.app.common.security.Role;
import com.example.app.modules.users.entity.UserStatus;
import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        Role role,
        UserStatus status,
        Instant createdAt) {
}
