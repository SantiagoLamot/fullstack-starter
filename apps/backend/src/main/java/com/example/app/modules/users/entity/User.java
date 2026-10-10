package com.example.app.modules.users.entity;

import com.example.app.common.persistence.BaseEntity;
import com.example.app.common.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * Application user (USER or ADMIN, see Role). There is no public sign-up: users are created
 * {@link UserStatus#INVITED} by an admin and become {@link UserStatus#ACTIVE} when they set a password.
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User extends BaseEntity {

    public static final int EMAIL_MAX_LENGTH = 254;
    public static final int NAME_MAX_LENGTH = 100;
    public static final int PHONE_MAX_LENGTH = 30;
    /** Phone: international number, digits only. */
    public static final String PHONE_REGEX = "(\\d{8,15})?";
    public static final String PHONE_MESSAGE = "debe tener solo dígitos (entre 8 y 15), sin espacios ni símbolos";

    @Column(name = "email", nullable = false, length = EMAIL_MAX_LENGTH)
    private String email;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = NAME_MAX_LENGTH)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = NAME_MAX_LENGTH)
    private String lastName;

    @Column(name = "phone", length = PHONE_MAX_LENGTH)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected User() {
        // JPA
    }

    private User(String email, String firstName, String lastName, String phone, Role role) {
        this.email = normalizeEmail(email);
        this.firstName = Objects.requireNonNull(firstName, "firstName").strip();
        this.lastName = Objects.requireNonNull(lastName, "lastName").strip();
        this.phone = blankToNull(phone);
        this.role = Objects.requireNonNull(role, "role");
        this.status = UserStatus.INVITED;
    }

    /** New invited user, without password until the invitation is accepted. */
    public static User invite(String email, String firstName, String lastName, String phone, Role role) {
        return new User(email, firstName, lastName, phone, role);
    }

    public static String normalizeEmail(String email) {
        return Objects.requireNonNull(email, "email").strip().toLowerCase(Locale.ROOT);
    }

    /** Sets the first password and activates the account (invitation accepted). */
    public void activate(String newPasswordHash) {
        if (status != UserStatus.INVITED) {
            throw new IllegalStateException("Only invited users can be activated");
        }
        this.passwordHash = Objects.requireNonNull(newPasswordHash, "passwordHash");
        this.status = UserStatus.ACTIVE;
        resetFailedLogins();
    }

    /** Password reset by email link: new hash and the failed-login lock is cleared. */
    public void resetPassword(String newPasswordHash) {
        changePassword(newPasswordHash);
        resetFailedLogins();
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash, "passwordHash");
    }

    /** Partial update: null leaves a field untouched; a blank phone clears it. */
    public void updateProfile(String firstName, String lastName, String phone) {
        if (firstName != null) {
            this.firstName = firstName.strip();
        }
        if (lastName != null) {
            this.lastName = lastName.strip();
        }
        if (phone != null) {
            this.phone = blankToNull(phone);
        }
    }

    public void changeRole(Role newRole) {
        this.role = Objects.requireNonNull(newRole, "role");
    }

    public void disable() {
        this.status = UserStatus.DISABLED;
    }

    /** Re-enables a disabled account: back to ACTIVE if it already had a password, INVITED otherwise. */
    public void enable() {
        if (status == UserStatus.DISABLED) {
            this.status = passwordHash != null ? UserStatus.ACTIVE : UserStatus.INVITED;
            resetFailedLogins();
        }
    }

    /** Counts a failed login and locks the account for {@code lockDuration} after {@code maxAttempts}. */
    public void registerFailedLogin(int maxAttempts, Duration lockDuration, Instant now) {
        this.failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            this.lockedUntil = now.plus(lockDuration);
            this.failedLoginAttempts = 0;
        }
    }

    public void resetFailedLogins() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public boolean canLogin(Instant now) {
        return status == UserStatus.ACTIVE && passwordHash != null && !isLocked(now);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public Role getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public long getVersion() {
        return version;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
