package com.example.app.support;

import java.time.Instant;
import java.time.ZoneId;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Replaces the application Clock with a {@link MutableClock} (zone deliberately NOT Buenos Aires: UTC). */
@TestConfiguration(proxyBeanMethods = false)
public class MutableClockConfiguration {

    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock(Instant.parse("2026-10-02T18:00:00Z"), ZoneId.of("UTC"));
    }
}
