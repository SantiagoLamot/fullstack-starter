package com.example.app.modules.users.service;

import com.example.app.common.exception.BusinessException;
import com.example.app.common.exception.NotFoundException;
import com.example.app.common.security.Role;
import com.example.app.config.AppProperties;
import com.example.app.modules.users.entity.User;
import com.example.app.modules.users.entity.UserStatus;
import com.example.app.modules.users.repository.UserRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountServiceImpl implements UserAccountService {

    private final UserRepository userRepository;
    private final AppProperties properties;
    private final Clock clock;

    public UserAccountServiceImpl(UserRepository userRepository, AppProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(User.normalizeEmail(email));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    @Override
    @Transactional
    public void registerFailedLogin(Long userId) {
        userRepository.findById(userId).ifPresent(user -> user.registerFailedLogin(
                properties.security().login().maxAttempts(),
                properties.security().login().lockDuration(),
                clock.instant()));
    }

    @Override
    @Transactional
    public void registerSuccessfulLogin(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
                user.resetFailedLogins();
            }
        });
    }

    @Override
    @Transactional
    public void activate(Long userId, String passwordHash) {
        User user = userRepository.findById(userId)
                .filter(found -> found.getStatus() == UserStatus.INVITED)
                .orElseThrow(() -> new BusinessException("La invitación es inválida o ya venció."));
        user.activate(passwordHash);
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, String passwordHash) {
        require(userId).resetPassword(passwordHash);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String passwordHash) {
        require(userId).changePassword(passwordHash);
    }

    private User require(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("El usuario no existe."));
    }
}
