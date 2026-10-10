package com.example.app.modules.auth.dto.request;

import com.example.app.common.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 72) @MaxUtf8Bytes(max = MaxUtf8Bytes.BCRYPT) String password) {

    @Override
    public String toString() {
        return "LoginRequest[email=***, password=***]";
    }
}
