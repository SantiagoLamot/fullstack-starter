package com.example.app.modules.auth.service;

import com.example.app.modules.auth.dto.response.AuthUserResponse;

/** Result of login/refresh before the controller decides how the refresh token travels (body or cookie). */
public record AuthSession(String accessToken, long expiresInSeconds, String refreshToken, AuthUserResponse user) {

    @Override
    public String toString() {
        return "AuthSession[user=" + user + "]";
    }
}
