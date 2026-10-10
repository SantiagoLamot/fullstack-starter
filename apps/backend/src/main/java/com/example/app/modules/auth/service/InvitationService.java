package com.example.app.modules.auth.service;

public interface InvitationService {

    /**
     * Invalidates the pending invitations of the user, creates a new single-use token and sends the mail
     * (after the surrounding transaction commits).
     */
    void issue(Long userId, String email, String firstName);

    /** Consumes the token and activates the account with the chosen password. Invalid/expired/used: 422. */
    void accept(String rawToken, String rawPassword);
}
