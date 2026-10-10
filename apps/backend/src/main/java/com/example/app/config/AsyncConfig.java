package com.example.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Enables {@code @Async} (Boot's default {@code applicationTaskExecutor}). Used to send notifications off the request. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
