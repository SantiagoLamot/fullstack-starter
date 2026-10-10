package com.example.app.modules.users.dto.request;

import com.example.app.common.security.Role;
import com.example.app.modules.users.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Admin creates an invited user; the user then sets the password through the invitation link. */
public record CreateUserRequest(
        @NotBlank @Email @Size(max = User.EMAIL_MAX_LENGTH) String email,
        @NotBlank @Size(max = User.NAME_MAX_LENGTH) String firstName,
        @NotBlank @Size(max = User.NAME_MAX_LENGTH) String lastName,
        @Pattern(regexp = User.PHONE_REGEX, message = User.PHONE_MESSAGE) String phone,
        @NotNull Role role) {
}
