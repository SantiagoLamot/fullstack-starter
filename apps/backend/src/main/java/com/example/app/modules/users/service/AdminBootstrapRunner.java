package com.example.app.modules.users.service;

import com.example.app.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** At startup, creates the first ADMIN (invited by mail) if none exists and ADMIN_BOOTSTRAP_EMAIL is set. */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AppProperties properties;
    private final UserAdminService userAdminService;

    public AdminBootstrapRunner(AppProperties properties, UserAdminService userAdminService) {
        this.properties = properties;
        this.userAdminService = userAdminService;
    }

    @Override
    public void run(ApplicationArguments args) {
        AppProperties.Bootstrap bootstrap = properties.bootstrap();
        String email = bootstrap.adminEmail();
        if (email == null || email.isBlank()) {
            return;
        }
        boolean created = userAdminService.bootstrapFirstAdmin(email,
                orDefault(bootstrap.adminFirstName(), "Admin"),
                orDefault(bootstrap.adminLastName(), "App"));
        if (created) {
            log.info("Initial ADMIN created and invited (no ADMIN existed).");
        }
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
