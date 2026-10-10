package com.example.app.modules.auth.service;

import com.example.app.config.AppProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory fixed-window limiter by key (prefixes {@code reset-ip:}, {@code reset-email:},
 * {@code reset-confirm-ip:}, {@code pwd-change:}). Valid with a single instance (ADR 0005). Expired windows
 * are purged on write once the map grows; "now" comes from the injected {@link Clock}.
 */
@Component
public class RateLimiter {

    static final int PURGE_THRESHOLD = 1_000;

    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Counts one attempt; false when it goes over the limit (the attempt is still counted). */
    public boolean tryAcquire(String key, AppProperties.Limit limit) {
        return record(key, limit).count() <= limit.maxAttempts();
    }

    /** True when the key already used every attempt of its current window (nothing is counted). */
    public boolean isExhausted(String key, AppProperties.Limit limit) {
        Window window = windows.get(key);
        return window != null && window.expiresAt().isAfter(clock.instant())
                && window.count() >= limit.maxAttempts();
    }

    /** Counts one attempt (e.g. only failed password changes count). */
    public void recordAttempt(String key, AppProperties.Limit limit) {
        record(key, limit);
    }

    private Window record(String key, AppProperties.Limit limit) {
        Instant now = clock.instant();
        if (windows.size() >= PURGE_THRESHOLD) {
            windows.values().removeIf(window -> !window.expiresAt().isAfter(now));
        }
        return windows.compute(key, (k, current) -> current == null || !current.expiresAt().isAfter(now)
                ? new Window(1, now.plus(limit.window()))
                : new Window(current.count() + 1, current.expiresAt()));
    }

    private record Window(int count, Instant expiresAt) {
    }
}
