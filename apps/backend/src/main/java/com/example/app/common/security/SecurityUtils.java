package com.example.app.common.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** Access to the authenticated user of the current request. */
public final class SecurityUtils {

    /** Access token claim with the session id (= refresh token family id, ADR 0009). */
    public static final String SESSION_ID_CLAIM = "sid";

    private SecurityUtils() {
    }

    /** Session id from the JWT {@code sid} claim, empty if there is no JWT authentication or no claim. */
    public static Optional<String> currentSessionId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        String sessionId = jwt.getClaimAsString(SESSION_ID_CLAIM);
        return sessionId == null || sessionId.isBlank() ? Optional.empty() : Optional.of(sessionId);
    }

    /** User id from the JWT {@code sub} claim, empty if there is no JWT authentication or sub is not numeric. */
    public static Optional<Long> currentUserId() {
        return userId(SecurityContextHolder.getContext().getAuthentication());
    }

    public static Optional<Long> userId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        String subject = jwt.getSubject();
        if (subject == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(subject));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
