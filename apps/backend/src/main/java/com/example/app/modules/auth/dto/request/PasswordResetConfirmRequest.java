package com.example.app.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank @Size(max = 200) String token,
        @ValidPassword String newPassword) {

    @Override
    public String toString() {
        return "PasswordResetConfirmRequest[token=***, newPassword=***]";
    }
}
