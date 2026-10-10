package com.example.app.modules.auth.dto.request;

import com.example.app.common.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(max = 72) @MaxUtf8Bytes(max = MaxUtf8Bytes.BCRYPT) String currentPassword,
        @ValidPassword String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordRequest[currentPassword=***, newPassword=***]";
    }
}
