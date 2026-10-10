package com.example.app.modules.users.dto.request;

import com.example.app.modules.users.entity.User;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Partial update: null fields are left untouched. Email and role are not editable. A blank
 * {@code phone} clears the value.
 */
public record UpdateUserRequest(
        @Size(max = User.NAME_MAX_LENGTH) @Pattern(regexp = ".*\\S.*") String firstName,
        @Size(max = User.NAME_MAX_LENGTH) @Pattern(regexp = ".*\\S.*") String lastName,
        @Pattern(regexp = User.PHONE_REGEX, message = User.PHONE_MESSAGE) String phone) {
}
