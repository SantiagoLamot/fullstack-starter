package com.example.app.modules.users.service;

import com.example.app.modules.users.entity.User;
import java.util.List;
import java.util.Optional;

/** Account operations that other modules (auth) need from users. */
public interface UserAccountService {

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long userId);

    /** Counts a failed login; locks the account after the configured number of attempts. */
    void registerFailedLogin(Long userId);

    void registerSuccessfulLogin(Long userId);

    /** Sets the first password (already hashed) and activates the invited account. */
    void activate(Long userId, String passwordHash);

    /** Password reset by email link (hash already computed): replaces the hash and unlocks the account. */
    void resetPassword(Long userId, String passwordHash);

    /** Password change from "Mi cuenta" (hash already computed). Does not touch the failed-login counter. */
    void changePassword(Long userId, String passwordHash);

}
