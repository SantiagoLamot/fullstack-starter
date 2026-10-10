package com.example.app.modules.auth.controller;

import com.example.app.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Web/mobile split for the refresh token. A request carrying the custom header {@code X-Requested-With}
 * is a web (admin-web) request: the refresh token travels in an HttpOnly cookie. Anything else (mobile)
 * uses the request/response body. The header also acts as an extra CSRF defense on the cookie flow (a
 * cross-site form cannot set it).
 */
@Component
public class RefreshCookies {

    public static final String COOKIE_NAME = "refresh_token";
    public static final String COOKIE_PATH = "/api/v1/auth";
    public static final String WEB_CLIENT_HEADER = "X-Requested-With";

    private final AppProperties properties;

    public RefreshCookies(AppProperties properties) {
        this.properties = properties;
    }

    public boolean isWebClient(HttpServletRequest request) {
        String header = request.getHeader(WEB_CLIENT_HEADER);
        return header != null && !header.isBlank();
    }

    /** The refresh token cookie value, or null when absent. */
    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public String issue(String refreshToken) {
        return build(refreshToken, properties.security().jwt().refreshTokenTtl()).toString();
    }

    public String clear() {
        return build("", Duration.ZERO).toString();
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
