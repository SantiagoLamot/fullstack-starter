package com.example.app.common.security;

/** Application roles. Carried in the JWT {@code roles} claim and mapped to {@code ROLE_*} authorities. */
public enum Role {
    /** Regular app user (mobile app / self-service endpoints under {@code /me/**}). */
    USER,
    /** Administrator (admin panel, endpoints under {@code /admin/**}). */
    ADMIN;

    public static final String AUTHORITY_PREFIX = "ROLE_";

    public String authority() {
        return AUTHORITY_PREFIX + name();
    }
}
