package com.example.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/** The typed {@code app.*} settings are validated at startup: a missing value stops the application. */
class AppPropertiesValidationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(PropertiesConfig.class)
            .withPropertyValues(
                    "app.name=App",
                    "app.time-zone=America/Argentina/Buenos_Aires",
                    "app.public-web-url=http://localhost:5173",
                    "app.cors.allowed-origins=http://localhost:5173,http://localhost:8088",
                    "app.mail.from=no-reply@example.local",
                    "app.mail.admin-notify=",
                    "app.security.jwt.issuer=app",
                    "app.security.jwt.access-token-ttl=PT15M",
                    "app.security.jwt.refresh-token-ttl=P30D",
                    "app.security.login.max-attempts=5",
                    "app.security.login.lock-duration=PT15M",
                    "app.security.invitation-ttl=PT72H",
                    "app.security.password-reset-ttl=PT1H",
                    "app.security.session-check-cache-ttl=PT30S",
                    "app.security.rate-limit.reset-per-ip.max-attempts=5",
                    "app.security.rate-limit.reset-per-ip.window=PT15M",
                    "app.security.rate-limit.reset-per-email.max-attempts=3",
                    "app.security.rate-limit.reset-per-email.window=PT1H",
                    "app.security.rate-limit.confirm-per-ip.max-attempts=10",
                    "app.security.rate-limit.confirm-per-ip.window=PT15M",
                    "app.security.rate-limit.password-change-per-user.max-attempts=5",
                    "app.security.rate-limit.password-change-per-user.window=PT15M",
                    "app.bootstrap.admin-email=",
                    "app.dev-seed.admin-email=");

    @Test
    void bindsValidConfiguration() {
        runner.withPropertyValues("app.security.jwt.secret=c2VjcmV0").run(context -> {
            assertThat(context).hasNotFailed();
            AppProperties properties = context.getBean(AppProperties.class);
            assertThat(properties.cors().allowedOrigins()).hasSize(2);
            assertThat(properties.security().invitationTtl()).isEqualTo(Duration.ofHours(72));
            assertThat(properties.security().jwt().toString()).doesNotContain("c2VjcmV0");
        });
    }

    @Test
    void missingJwtSecretStopsStartup() {
        runner.withPropertyValues("app.security.jwt.secret=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("JWT_SECRET");
        });
    }

    @Test
    void nonPositiveResetTtlOrNegativeSessionCacheTtlStopsStartup() {
        runner.withPropertyValues("app.security.jwt.secret=c2VjcmV0", "app.security.password-reset-ttl=PT0S")
                .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("app.security.jwt.secret=c2VjcmV0", "app.security.session-check-cache-ttl=-PT1S")
                .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("app.security.jwt.secret=c2VjcmV0", "app.security.session-check-cache-ttl=PT0S")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void invalidLoginAttemptsStopsStartup() {
        runner.withPropertyValues("app.security.jwt.secret=c2VjcmV0", "app.security.login.max-attempts=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AppProperties.class)
    static class PropertiesConfig {
    }
}
