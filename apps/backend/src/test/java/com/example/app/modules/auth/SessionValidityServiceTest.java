package com.example.app.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.app.config.AppProperties;
import com.example.app.modules.auth.entity.RefreshToken;
import com.example.app.modules.auth.repository.RefreshTokenRepository;
import com.example.app.modules.auth.service.SessionValidityService;
import com.example.app.modules.auth.service.SessionsRevokedEvent;
import com.example.app.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SessionValidityServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");

    private final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    private final MutableClock clock = new MutableClock(NOW, ZoneId.of("UTC"));

    @Test
    void positiveAnswersAreCachedUntilTheTtlOrAnEviction() {
        SessionValidityService service = service(Duration.ofSeconds(30));
        live("fam-1", 7L);

        assertThat(service.isActive("fam-1")).isTrue();
        assertThat(service.isActive("fam-1")).isTrue();
        verify(repository, times(1)).findFirstByFamilyIdAndRevokedAtIsNullAndExpiresAtAfter(eq("fam-1"), any());

        clock.set(NOW.plusSeconds(31));
        assertThat(service.isActive("fam-1")).isTrue();
        verify(repository, times(2)).findFirstByFamilyIdAndRevokedAtIsNullAndExpiresAtAfter(eq("fam-1"), any());

        // revocation of that family: evicted, the next check sees the database
        when(repository.findFirstByFamilyIdAndRevokedAtIsNullAndExpiresAtAfter(eq("fam-1"), any()))
                .thenReturn(Optional.empty());
        service.onSessionsRevoked(new SessionsRevokedEvent(7L, "fam-1"));
        assertThat(service.isActive("fam-1")).isFalse();
    }

    @Test
    void revokingEveryUserSessionEvictsAllItsFamiliesOnly() {
        SessionValidityService service = service(Duration.ofSeconds(30));
        live("fam-a", 1L);
        live("fam-b", 1L);
        live("fam-c", 2L);
        service.isActive("fam-a");
        service.isActive("fam-b");
        service.isActive("fam-c");

        service.onSessionsRevoked(new SessionsRevokedEvent(1L, null));
        service.isActive("fam-a");
        service.isActive("fam-b");
        service.isActive("fam-c");

        verify(repository, times(2)).findFirstByFamilyIdAndRevokedAtIsNullAndExpiresAtAfter(eq("fam-a"), any());
        verify(repository, times(2)).findFirstByFamilyIdAndRevokedAtIsNullAndExpiresAtAfter(eq("fam-b"), any());
        verify(repository, times(1)).findFirstByFamilyIdAndRevokedAtIsNullAndExpiresAtAfter(eq("fam-c"), any());
    }

    @Test
    void zeroTtlDisablesTheCacheAndNegativesAreNeverCached() {
        SessionValidityService service = service(Duration.ZERO);
        live("fam-1", 7L);
        service.isActive("fam-1");
        service.isActive("fam-1");
        verify(repository, times(2)).findFirstByFamilyIdAndRevokedAtIsNullAndExpiresAtAfter(eq("fam-1"), any());

        assertThat(service.isActive("unknown")).isFalse();
        assertThat(service.isActive("unknown")).isFalse();
        verify(repository, times(2)).findFirstByFamilyIdAndRevokedAtIsNullAndExpiresAtAfter(eq("unknown"), any());
    }

    private void live(String familyId, Long userId) {
        when(repository.findFirstByFamilyIdAndRevokedAtIsNullAndExpiresAtAfter(eq(familyId), any()))
                .thenReturn(Optional.of(new RefreshToken(userId, "hash-" + familyId, familyId, NOW.plusSeconds(3600))));
    }

    private SessionValidityService service(Duration ttl) {
        AppProperties.Security security = new AppProperties.Security(null, null, Duration.ofHours(72),
                Duration.ofHours(1), ttl, null);
        AppProperties properties = new AppProperties(null, null, null, null, null, security, null, null);
        return new SessionValidityService(repository, properties, clock);
    }
}
