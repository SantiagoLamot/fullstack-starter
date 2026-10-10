package com.example.app.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(@NotBlank @Email @Size(max = 254) String email) {

    @Override
    public String toString() {
        return "PasswordResetRequest[email=***]";
    }
}
