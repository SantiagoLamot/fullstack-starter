package com.example.app.modules.auth.service;

public interface RefreshTokenService {

    /** Starts a new family (login): the family id is the session id (claim {@code sid}). */
    IssuedRefreshToken startFamily(Long userId);

    /**
     * Rotates a refresh token: revokes it and issues a new one of the same family. A token that was already
     * revoked revokes its whole family (possible theft). Invalid/expired/revoked: 401.
     */
    RotatedRefreshToken rotate(String rawToken);

    /**
     * Logout: revokes the family of the given token (even if it was already rotated or expired). Holding the
     * token proves the session, so no user is needed. Unknown tokens are ignored.
     */
    void revokeFamilyOf(String rawToken);

    void revokeAllOfUser(Long userId);

    /** Revokes every session of the user except {@code keptFamilyId} (password change keeps the current one). */
    void revokeAllOfUserExcept(Long userId, String keptFamilyId);

    record IssuedRefreshToken(String familyId, String rawToken) {

        @Override
        public String toString() {
            return "IssuedRefreshToken[familyId=" + familyId + ", rawToken=***]";
        }
    }

    record RotatedRefreshToken(Long userId, String familyId, String newToken) {

        @Override
        public String toString() {
            return "RotatedRefreshToken[userId=" + userId + ", familyId=" + familyId + ", newToken=***]";
        }
    }
}
