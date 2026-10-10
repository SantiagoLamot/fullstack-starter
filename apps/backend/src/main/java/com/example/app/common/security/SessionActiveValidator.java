package com.example.app.common.security;

import java.util.Objects;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Rejects (401) access tokens without claim {@code sid} or whose session was revoked (ADR 0009). Tokens
 * issued before the claim existed are rejected too: the client recovers with a refresh.
 */
public class SessionActiveValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INACTIVE_SESSION =
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "The session is no longer active", null);

    private final SessionValidityChecker checker;

    public SessionActiveValidator(SessionValidityChecker checker) {
        this.checker = Objects.requireNonNull(checker, "checker");
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String sessionId = jwt.getClaimAsString(SecurityUtils.SESSION_ID_CLAIM);
        if (sessionId == null || sessionId.isBlank() || !checker.isActive(sessionId)) {
            return OAuth2TokenValidatorResult.failure(INACTIVE_SESSION);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
