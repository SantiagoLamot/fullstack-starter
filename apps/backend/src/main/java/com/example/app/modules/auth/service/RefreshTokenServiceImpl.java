package com.example.app.modules.auth.service;

import com.example.app.common.exception.UnauthorizedException;
import com.example.app.config.AppProperties;
import com.example.app.modules.auth.entity.RefreshToken;
import com.example.app.modules.auth.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every revocation publishes {@link SessionsRevokedEvent} so the session cache is evicted after the commit. */
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    static final String INVALID_SESSION = "La sesión es inválida o expiró. Iniciá sesión de nuevo.";

    private final RefreshTokenRepository repository;
    private final OpaqueTokens opaqueTokens;
    private final ApplicationEventPublisher events;
    private final AppProperties properties;
    private final Clock clock;

    public RefreshTokenServiceImpl(RefreshTokenRepository repository, OpaqueTokens opaqueTokens,
            ApplicationEventPublisher events, AppProperties properties, Clock clock) {
        this.repository = repository;
        this.opaqueTokens = opaqueTokens;
        this.events = events;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public IssuedRefreshToken startFamily(Long userId) {
        String familyId = UUID.randomUUID().toString();
        return new IssuedRefreshToken(familyId, issue(userId, familyId));
    }

    /** {@code noRollbackFor}: the family revocation on reuse must be committed even though we answer 401. */
    @Override
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public RotatedRefreshToken rotate(String rawToken) {
        Instant now = clock.instant();
        RefreshToken current = repository.findByTokenHash(opaqueTokens.hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException(INVALID_SESSION));
        if (current.isRevoked()) {
            revokeFamily(current.getUserId(), current.getFamilyId(), now);
            throw new UnauthorizedException(INVALID_SESSION);
        }
        if (current.isExpired(now)) {
            throw new UnauthorizedException(INVALID_SESSION);
        }
        current.revoke(now);
        return new RotatedRefreshToken(current.getUserId(), current.getFamilyId(),
                issue(current.getUserId(), current.getFamilyId()));
    }

    @Override
    @Transactional
    public void revokeFamilyOf(String rawToken) {
        repository.findByTokenHash(opaqueTokens.hash(rawToken))
                .ifPresent(token -> revokeFamily(token.getUserId(), token.getFamilyId(), clock.instant()));
    }

    @Override
    @Transactional
    public void revokeAllOfUser(Long userId) {
        Instant now = clock.instant();
        repository.findByUserIdAndRevokedAtIsNull(userId).forEach(token -> token.revoke(now));
        events.publishEvent(new SessionsRevokedEvent(userId, null));
    }

    @Override
    @Transactional
    public void revokeAllOfUserExcept(Long userId, String keptFamilyId) {
        Instant now = clock.instant();
        repository.findByUserIdAndRevokedAtIsNull(userId).stream()
                .filter(token -> !token.getFamilyId().equals(keptFamilyId))
                .forEach(token -> token.revoke(now));
        // Evicting the kept session too is harmless: it is re-checked (and still active) on the next request.
        events.publishEvent(new SessionsRevokedEvent(userId, null));
    }

    private String issue(Long userId, String familyId) {
        String raw = opaqueTokens.generate();
        Instant expiresAt = clock.instant().plus(properties.security().jwt().refreshTokenTtl());
        repository.save(new RefreshToken(userId, opaqueTokens.hash(raw), familyId, expiresAt));
        return raw;
    }

    private void revokeFamily(Long userId, String familyId, Instant now) {
        repository.findByFamilyIdAndRevokedAtIsNull(familyId).forEach(token -> token.revoke(now));
        events.publishEvent(new SessionsRevokedEvent(userId, familyId));
    }
}
