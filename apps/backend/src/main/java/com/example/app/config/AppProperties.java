package com.example.app.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed application settings ({@code app.*}), bound from environment variables and validated at startup.
 * {@code name} is the product name shown in e-mails (APP_NAME).
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String name,
        @NotNull ZoneId timeZone,
        @NotNull URI publicWebUrl,
        @Valid @NotNull Cors cors,
        @Valid @NotNull Mail mail,
        @Valid @NotNull Security security,
        @Valid @NotNull Bootstrap bootstrap,
        @Valid @NotNull DevSeed devSeed) {

    public record Cors(@NotEmpty List<@NotBlank String> allowedOrigins) {
    }

    /**
     * {@code adminNotify}: optional destination of admin notices; empty = only logged. {@code enabled}
     * false: no mail leaves the app (only a log line without addresses or content).
     */
    public record Mail(@NotBlank @Email String from, @Email String adminNotify, boolean enabled) {
    }

    public record Security(
            @Valid @NotNull Jwt jwt,
            @Valid @NotNull Login login,
            @NotNull Duration invitationTtl,
            @NotNull Duration passwordResetTtl,
            @NotNull Duration sessionCheckCacheTtl,
            @Valid @NotNull RateLimit rateLimit) {

        @AssertTrue(message = "password-reset-ttl must be positive and session-check-cache-ttl zero or positive")
        public boolean isDurationsValid() {
            return passwordResetTtl == null || sessionCheckCacheTtl == null
                    || (passwordResetTtl.isPositive() && !sessionCheckCacheTtl.isNegative());
        }
    }

    /** In-memory fixed-window limits (single instance, ADR 0005). */
    public record RateLimit(
            @Valid @NotNull Limit resetPerIp,
            @Valid @NotNull Limit resetPerEmail,
            @Valid @NotNull Limit confirmPerIp,
            @Valid @NotNull Limit passwordChangePerUser) {
    }

    public record Limit(@Min(1) int maxAttempts, @NotNull Duration window) {
    }

    public record Jwt(
            @NotBlank(message = "{app.security.jwt.secret.required}") String secret,
            @NotBlank String issuer,
            @NotNull Duration accessTokenTtl,
            @NotNull Duration refreshTokenTtl) {

        @Override
        public String toString() {
            return "Jwt[secret=***, issuer=" + issuer + ", accessTokenTtl=" + accessTokenTtl
                    + ", refreshTokenTtl=" + refreshTokenTtl + "]";
        }
    }

    public record Login(@Min(1) int maxAttempts, @NotNull Duration lockDuration) {
    }

    /**
     * First admin created at startup (every profile) when no ADMIN exists. All optional: without an email
     * nothing is created. No password: the admin receives an invitation.
     */
    public record Bootstrap(@Email String adminEmail, String adminFirstName, String adminLastName) {
    }

    /** Known-credentials users created at startup ONLY in the dev profile. Empty password = user not created. */
    public record DevSeed(
            @Email String adminEmail,
            String adminPassword,
            @Email String userEmail,
            String userPassword) {

        @Override
        public String toString() {
            return "DevSeed[adminEmail=" + adminEmail + ", adminPassword=***, userEmail=" + userEmail
                    + ", userPassword=***]";
        }
    }
}
