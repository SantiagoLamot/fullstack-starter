package com.example.app.modules.auth.entity;

import com.example.app.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Single-use password reset token, stored as SHA-256 hash. Kept apart from {@link InvitationToken}: TTL,
 * mail and account-state rules differ.
 */
@Entity
@Table(name = "password_reset_token",
        uniqueConstraints = @UniqueConstraint(name = "uk_password_reset_token_hash", columnNames = "token_hash"),
        indexes = @Index(name = "ix_password_reset_token_user", columnList = "user_id"))
public class PasswordResetToken extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    protected PasswordResetToken() {
        // JPA
    }

    public PasswordResetToken(Long userId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    /** Marks it as consumed (password reset, or superseded by a newer request). */
    public void markUsed(Instant now) {
        if (usedAt == null) {
            this.usedAt = now;
        }
    }

    public boolean isUsable(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    public Long getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }
}
