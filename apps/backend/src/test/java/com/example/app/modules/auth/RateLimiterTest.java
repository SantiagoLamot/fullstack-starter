package com.example.app.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.app.config.AppProperties;
import com.example.app.modules.auth.service.RateLimiter;
import com.example.app.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private static final Instant START = Instant.parse("2026-10-03T12:00:00Z");
    private static final AppProperties.Limit THREE_PER_15_MIN = new AppProperties.Limit(3, Duration.ofMinutes(15));

    private final MutableClock clock = new MutableClock(START, ZoneId.of("UTC"));
    private final RateLimiter limiter = new RateLimiter(clock);

    @Test
    void allowsUpToTheLimitPerKeyAndResetsWhenTheWindowEnds() {
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("reset-ip:a", THREE_PER_15_MIN)).isTrue();
        }
        assertThat(limiter.tryAcquire("reset-ip:a", THREE_PER_15_MIN)).isFalse();
        assertThat(limiter.tryAcquire("reset-ip:b", THREE_PER_15_MIN)).isTrue();

        clock.set(START.plus(Duration.ofMinutes(15)));
        assertThat(limiter.tryAcquire("reset-ip:a", THREE_PER_15_MIN)).isTrue();
    }

    @Test
    void onlyRecordedAttemptsCountTowardsExhaustion() {
        assertThat(limiter.isExhausted("pwd-change:1", THREE_PER_15_MIN)).isFalse();
        limiter.recordAttempt("pwd-change:1", THREE_PER_15_MIN);
        limiter.recordAttempt("pwd-change:1", THREE_PER_15_MIN);
        assertThat(limiter.isExhausted("pwd-change:1", THREE_PER_15_MIN)).isFalse();
        limiter.recordAttempt("pwd-change:1", THREE_PER_15_MIN);
        assertThat(limiter.isExhausted("pwd-change:1", THREE_PER_15_MIN)).isTrue();

        clock.set(START.plus(Duration.ofMinutes(16)));
        assertThat(limiter.isExhausted("pwd-change:1", THREE_PER_15_MIN)).isFalse();
    }
}
