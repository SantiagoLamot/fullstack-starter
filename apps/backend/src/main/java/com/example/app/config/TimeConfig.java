package com.example.app.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Single source of "now": inject {@link Clock} instead of calling {@code Instant.now()} (testable). */
@Configuration(proxyBeanMethods = false)
public class TimeConfig {

    @Bean
    Clock clock(AppProperties properties) {
        return Clock.system(properties.timeZone());
    }
}
