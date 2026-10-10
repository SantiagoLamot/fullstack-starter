package com.example.app.modules.auth.dto.request;

import jakarta.validation.constraints.Size;

/** Mobile clients send the refresh token to revoke; web clients use the cookie. Optional. */
public record LogoutRequest(@Size(max = 200) String refreshToken) {

    @Override
    public String toString() {
        return "LogoutRequest[refreshToken=***]";
    }
}
