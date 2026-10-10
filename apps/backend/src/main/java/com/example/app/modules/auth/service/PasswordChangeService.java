package com.example.app.modules.auth.service;

public interface PasswordChangeService {

    /**
     * Changes the password of the authenticated user, keeps the current session ({@code sessionId}) and revokes
     * every other one. Wrong current password or new = current: 422 (never counts as a failed login).
     * Too many wrong current passwords: 429.
     */
    void change(Long userId, String sessionId, String currentPassword, String newPassword);
}
