package com.example.app.common.security;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Builds the HMAC key used to sign and verify JWTs from the Base64 secret ({@code JWT_SECRET}).
 * Fails fast with a clear message when the secret is missing, not Base64 or shorter than 256 bits.
 */
public final class JwtSecrets {

    public static final int MIN_SECRET_BYTES = 32;
    public static final String HMAC_ALGORITHM = "HmacSHA256";

    private JwtSecrets() {
    }

    public static SecretKey hmacSha256Key(String base64Secret) {
        if (base64Secret == null || base64Secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET (app.security.jwt.secret) is not set. Generate one with: openssl rand -base64 48");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Secret.strip());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "JWT_SECRET (app.security.jwt.secret) must be Base64 encoded. Generate one with: openssl rand -base64 48",
                    ex);
        }
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET (app.security.jwt.secret) is too short: decoded length is "
                    + keyBytes.length + " bytes, at least " + MIN_SECRET_BYTES
                    + " bytes (256 bits) are required for HS256. Generate one with: openssl rand -base64 48");
        }
        return new SecretKeySpec(keyBytes, HMAC_ALGORITHM);
    }
}
