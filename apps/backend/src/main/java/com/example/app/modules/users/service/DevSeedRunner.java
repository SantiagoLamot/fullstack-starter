package com.example.app.modules.users.service;

import com.example.app.common.security.Role;
import com.example.app.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Dev profile only: creates an ACTIVE ADMIN and USER user with known credentials (from DEV_SEED_* variables) if
 * their emails do not exist; an INVITED user with that email and role but no password (for example created by
 * ADMIN_BOOTSTRAP_EMAIL) gets the seed password and is activated. Runs before {@link AdminBootstrapRunner}, so when it creates the ADMIN the
 * bootstrap finds an ADMIN already present and sends no duplicate invitation.
 */
@Component
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DevSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevSeedRunner.class);

    private final AppProperties properties;
    private final UserAdminService userAdminService;

    public DevSeedRunner(AppProperties properties, UserAdminService userAdminService) {
        this.properties = properties;
        this.userAdminService = userAdminService;
    }

    @Override
    public void run(ApplicationArguments args) {
        AppProperties.DevSeed seed = properties.devSeed();
        seed("ADMIN", seed.adminEmail(), seed.adminPassword(), "Admin", "Dev", Role.ADMIN, "DEV_SEED_ADMIN_PASSWORD");
        seed("USER", seed.userEmail(), seed.userPassword(), "Usuario", "Dev", Role.USER, "DEV_SEED_USER_PASSWORD");
    }

    private void seed(String label, String email, String password, String firstName, String lastName, Role role,
            String passwordVariable) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("Dev seed: {} user not created ({} or its email is empty).", label, passwordVariable);
            return;
        }
        if (userAdminService.createActiveIfAbsent(email, firstName, lastName, role, password)) {
            log.info("Dev seed: {} user {} ready (ACTIVE).", label, email);
        }
    }
}
