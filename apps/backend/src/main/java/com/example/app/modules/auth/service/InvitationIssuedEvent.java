package com.example.app.modules.auth.service;

/** Published when an invitation is created; {@code rawToken} goes only into the mail, never into logs. */
public record InvitationIssuedEvent(String email, String firstName, String rawToken) {

    @Override
    public String toString() {
        return "InvitationIssuedEvent[email=***, rawToken=***]";
    }
}
