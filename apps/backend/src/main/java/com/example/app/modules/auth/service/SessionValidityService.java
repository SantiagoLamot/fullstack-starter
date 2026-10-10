package com.example.app.modules.auth.service;

import com.example.app.common.security.SessionValidityChecker;
import com.example.app.config.AppProperties;
import com.example.app.modules.auth.entity.RefreshToken;
import com.example.app.modules.auth.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * A session (claim {@code sid} = refresh family) is active while its family has a non revoked, non expired
 * refresh token (ADR 0009). Only positive answers are cached, for {@code app.security.session-check-cache-ttl}
 * ({@code PT0S} disables the cache), and every revocation evicts after its commit, so with one instance a
 * logout or a password change takes effect on the very next request.
 */
@Service
public class SessionValidityService implements SessionValidityChecker {

    static final int MAX_ENTRIES = 10_000;

    private final RefreshTokenRepository repository;
    private final Duration cacheTtl;
    private final Clock clock;
    private final Map<String, CachedSession> cache = new ConcurrentHashMap<>();
    /**
     * Bumped on every eviction. A lookup that started before an eviction does not store its (possibly
     * stale) positive answer, so a revocation can never be undone by a concurrent check.
     */
    private final AtomicLong generation = new AtomicLong();

    public SessionValidityService(RefreshTokenRepository repository, AppProperties properties, Clock clock) {
        this.repository = repository;
        this.cacheTtl = properties.security().sessionCheckCacheTtl();
        this.clock = clock;
    }

    @Override
    public boolean isActive(String sessionId) {
        Instant now = clock.instant();
        CachedSession cached = cache.get(sessionId);
        if (cached != null && cached.validUntil().isAfter(now)) {
            return true;
        }
        long startedAt = generation.get();
        Optional<RefreshToken> live =
                repository.findFirstByFamilyIdAndRevokedAtIsNullAndExpiresAtAfter(sessionId, now);
        if (live.isEmpty()) {
            cache.remove(sessionId);
            return false;
        }
        if (cacheTtl.isPositive()) {
            store(sessionId, new CachedSession(live.get().getUserId(), now.plus(cacheTtl)), startedAt, now);
        }
        return true;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSessionsRevoked(SessionsRevokedEvent event) {
        generation.incrementAndGet();
        if (event.familyId() != null) {
            cache.remove(event.familyId());
        } else {
            cache.values().removeIf(entry -> entry.userId().equals(event.userId()));
        }
    }

    private void store(String sessionId, CachedSession entry, long startedAt, Instant now) {
        if (cache.size() >= MAX_ENTRIES) {
            cache.values().removeIf(existing -> !existing.validUntil().isAfter(now));
            if (cache.size() >= MAX_ENTRIES) {
                cache.clear();
            }
        }
        cache.put(sessionId, entry);
        if (generation.get() != startedAt) {
            // An eviction ran while we were reading the database: drop what we may have just cached.
            cache.remove(sessionId);
        }
    }

    private record CachedSession(Long userId, Instant validUntil) {
    }
}
