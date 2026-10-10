package com.example.app.modules.auth.service;

/**
 * Published by every refresh token revocation. {@code familyId} null = every session of the user (or every
 * session but the one kept on a password change). Consumed after the commit to evict the session cache.
 */
public record SessionsRevokedEvent(Long userId, String familyId) {
}
