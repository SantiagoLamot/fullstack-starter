package com.example.app.modules.users.entity;

/** Lifecycle of an account: created by invitation, activated when the invitation is accepted. */
public enum UserStatus {
    INVITED,
    ACTIVE,
    DISABLED
}
