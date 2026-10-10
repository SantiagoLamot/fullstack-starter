package com.example.app.modules.auth.service;

/** Published when a reset token is created; {@code rawToken} goes only into the mail, never into logs. */
public record PasswordResetRequestedEvent(String email, String firstName, String rawToken) {

    @Override
    public String toString() {
        return "PasswordResetRequestedEvent[email=***, rawToken=***]";
    }
}
