package com.example.app.modules.auth.entity;

import com.example.app.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/** Single-use invitation token, stored as SHA-256 hash. */
@Entity
@Table(name = "invitation_token",
        uniqueConstraints = @UniqueConstraint(name = "uk_invitation_token_hash", columnNames = "token_hash"),
        indexes = @Index(name = "ix_invitation_token_user", columnList = "user_id"))
public class InvitationToken extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    protected InvitationToken() {
        // JPA
    }

    public InvitationToken(Long userId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    /** Marks it as consumed (accepted, or superseded by a newer invitation). */
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
