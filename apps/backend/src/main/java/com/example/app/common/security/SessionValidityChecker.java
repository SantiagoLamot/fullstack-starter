package com.example.app.common.security;

/**
 * Port used by the JWT decoder to know whether the session behind an access token (claim {@code sid}) is
 * still alive. Implemented by the auth module (ADR 0009), so {@code config} does not depend on {@code auth}.
 */
public interface SessionValidityChecker {

    /** True when the session (refresh token family) still has a non revoked, non expired refresh token. */
    boolean isActive(String sessionId);
}
