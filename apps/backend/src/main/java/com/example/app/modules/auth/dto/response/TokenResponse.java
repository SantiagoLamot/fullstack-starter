package com.example.app.modules.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Login/refresh result. {@code refreshToken} is only present for mobile clients; web clients
 * (header {@code X-Requested-With}) receive it in an HttpOnly cookie instead.
 */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        @JsonInclude(JsonInclude.Include.NON_NULL) String refreshToken,
        AuthUserResponse user) {

    public static final String BEARER = "Bearer";
}
