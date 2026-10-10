package com.example.app.modules.auth.service;

import com.example.app.common.exception.BusinessException;
import com.example.app.common.exception.TooManyRequestsException;
import com.example.app.config.AppProperties;
import com.example.app.modules.auth.entity.PasswordResetToken;
import com.example.app.modules.auth.repository.PasswordResetTokenRepository;
import com.example.app.modules.users.entity.User;
import com.example.app.modules.users.entity.UserStatus;
import com.example.app.modules.users.service.UserAccountService;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    static final String INVALID_LINK = "El enlace es inválido o ya venció. Pedí uno nuevo.";

    private final PasswordResetTokenRepository repository;
    private final OpaqueTokens opaqueTokens;
    private final UserAccountService userAccountService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiter rateLimiter;
    private final ApplicationEventPublisher events;
    private final AppProperties properties;
    private final Clock clock;

    public PasswordResetServiceImpl(PasswordResetTokenRepository repository, OpaqueTokens opaqueTokens,
            UserAccountService userAccountService, RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder, RateLimiter rateLimiter, ApplicationEventPublisher events,
            AppProperties properties, Clock clock) {
        this.repository = repository;
        this.opaqueTokens = opaqueTokens;
        this.userAccountService = userAccountService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.events = events;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void request(String email, String clientIp) {
        AppProperties.RateLimit limits = properties.security().rateLimit();
        if (!rateLimiter.tryAcquire("reset-ip:" + clientIp, limits.resetPerIp())) {
            throw new TooManyRequestsException();
        }
        Optional<User> user = userAccountService.findByEmail(email).filter(PasswordResetServiceImpl::canReset);
        // Counted for every email (existing or not) and silent when exceeded: reveals nothing.
        if (!rateLimiter.tryAcquire("reset-email:" + User.normalizeEmail(email), limits.resetPerEmail())
                || user.isEmpty()) {
            return;
        }
        User account = user.get();
        Instant now = clock.instant();
        repository.findByUserIdAndUsedAtIsNull(account.getId()).forEach(token -> token.markUsed(now));
        String raw = opaqueTokens.generate();
        repository.save(new PasswordResetToken(account.getId(), opaqueTokens.hash(raw),
                now.plus(properties.security().passwordResetTtl())));
        events.publishEvent(new PasswordResetRequestedEvent(account.getEmail(), account.getFirstName(), raw));
    }

    /** {@code noRollbackFor}: a token of an account that is no longer active is burnt even though we answer 422. */
    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public void confirm(String rawToken, String newPassword, String clientIp) {
        if (!rateLimiter.tryAcquire("reset-confirm-ip:" + clientIp,
                properties.security().rateLimit().confirmPerIp())) {
            throw new TooManyRequestsException();
        }
        Instant now = clock.instant();
        PasswordResetToken token = repository.findWithLockByTokenHash(opaqueTokens.hash(rawToken))
                .filter(found -> found.isUsable(now))
                .orElseThrow(() -> new BusinessException(INVALID_LINK));
        Long userId = token.getUserId();
        if (userAccountService.findById(userId).filter(PasswordResetServiceImpl::canReset).isEmpty()) {
            token.markUsed(now);
            throw new BusinessException(INVALID_LINK);
        }
        userAccountService.resetPassword(userId, passwordEncoder.encode(newPassword));
        token.markUsed(now);
        repository.findByUserIdAndUsedAtIsNull(userId).forEach(other -> other.markUsed(now));
        refreshTokenService.revokeAllOfUser(userId);
    }

    /** ACTIVE with a password. A locked account can reset (it unlocks it). */
    private static boolean canReset(User user) {
        return user.getStatus() == UserStatus.ACTIVE && user.getPasswordHash() != null;
    }
}
