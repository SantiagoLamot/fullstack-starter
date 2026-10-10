package com.example.app.modules.auth.service;

public interface PasswordResetService {

    /**
     * Sends a reset link only to ACTIVE accounts. Never reveals whether the email exists:
     * returns normally in every case except the per-IP limit (429).
     */
    void request(String email, String clientIp);

    /**
     * Sets the new password, unlocks the account, consumes the token (and every other pending one) and revokes
     * every session. Unknown, used or expired token, or account no longer active: 422. Per-IP limit: 429.
     */
    void confirm(String rawToken, String newPassword, String clientIp);
}
