package com.example.app.modules.auth.service;

import com.example.app.common.exception.BusinessException;
import com.example.app.common.exception.TooManyRequestsException;
import com.example.app.common.exception.UnauthorizedException;
import com.example.app.config.AppProperties;
import com.example.app.modules.users.entity.User;
import com.example.app.modules.users.entity.UserStatus;
import com.example.app.modules.users.service.UserAccountService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordChangeServiceImpl implements PasswordChangeService {

    static final String WRONG_CURRENT = "La contraseña actual no es correcta.";
    static final String SAME_AS_CURRENT = "La contraseña nueva tiene que ser distinta de la actual.";

    private final UserAccountService userAccountService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiter rateLimiter;
    private final AppProperties properties;

    public PasswordChangeServiceImpl(UserAccountService userAccountService, RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder, RateLimiter rateLimiter, AppProperties properties) {
        this.userAccountService = userAccountService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void change(Long userId, String sessionId, String currentPassword, String newPassword) {
        String key = "pwd-change:" + userId;
        AppProperties.Limit limit = properties.security().rateLimit().passwordChangePerUser();
        if (rateLimiter.isExhausted(key, limit)) {
            throw new TooManyRequestsException();
        }
        User user = userAccountService.findById(userId)
                .filter(found -> found.getStatus() == UserStatus.ACTIVE && found.getPasswordHash() != null)
                .orElseThrow(() -> new UnauthorizedException(RefreshTokenServiceImpl.INVALID_SESSION));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            // Not a failed login (a stolen token must not lock the user out): only the per-user limit counts.
            rateLimiter.recordAttempt(key, limit);
            throw new BusinessException(WRONG_CURRENT, "currentPassword");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessException(SAME_AS_CURRENT, "newPassword");
        }
        userAccountService.changePassword(userId, passwordEncoder.encode(newPassword));
        refreshTokenService.revokeAllOfUserExcept(userId, sessionId);
    }
}
