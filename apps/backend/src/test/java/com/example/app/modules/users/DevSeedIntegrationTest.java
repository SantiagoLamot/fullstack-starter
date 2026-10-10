package com.example.app.modules.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.app.common.security.Role;
import com.example.app.modules.users.entity.User;
import com.example.app.modules.users.entity.UserStatus;
import com.example.app.modules.users.repository.UserRepository;
import com.example.app.modules.users.service.DevSeedRunner;
import com.example.app.modules.users.service.UserAdminService;
import com.example.app.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** The dev seed creates ACTIVE users with known credentials, is idempotent and does not clash with the bootstrap. */
@SpringBootTest(properties = {
        "app.dev-seed.admin-email=seed-admin@example.local",
        "app.dev-seed.admin-password=SeedAdmin123",
        "app.bootstrap.admin-email=bootstrap-admin@example.local"
})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles({"test", "dev"})
class DevSeedIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private DevSeedRunner runner;
    @Autowired
    private UserAdminService userAdminService;
    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void createsTheActiveAdminAndAvoidsDuplicateInvitation() {
        User admin = userRepository.findByEmailIgnoreCase("seed-admin@example.local").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(passwordEncoder.matches("SeedAdmin123", admin.getPasswordHash())).isTrue();

        // the seed ran first, so the bootstrap found an ADMIN and neither created nor invited anyone
        assertThat(userRepository.findByEmailIgnoreCase("bootstrap-admin@example.local")).isEmpty();
        verifyNoInteractions(mailSender);
    }

    @Test
    void anInvitedUserWithoutPasswordIsCompletedAndActivatedButActiveOrOtherRoleUsersAreNot() {
        String suffix = java.util.UUID.randomUUID().toString();
        String invited = "invited-" + suffix + "@example.local";
        String active = "active-" + suffix + "@example.local";
        String otherRole = "other-" + suffix + "@example.local";
        userRepository.save(User.invite(invited, "Admin", "Dev", null, Role.ADMIN));
        User activeUser = User.invite(active, "Admin", "Dev", null, Role.ADMIN);
        activeUser.activate(passwordEncoder.encode("KeepMe12345"));
        userRepository.save(activeUser);
        userRepository.save(User.invite(otherRole, "Other", "Dev", null, Role.USER));

        assertThat(userAdminService.createActiveIfAbsent(invited, "Admin", "Dev", Role.ADMIN, "SeedAdmin123")).isTrue();
        assertThat(userAdminService.createActiveIfAbsent(active, "Admin", "Dev", Role.ADMIN, "SeedAdmin123")).isFalse();
        assertThat(userAdminService.createActiveIfAbsent(otherRole, "Admin", "Dev", Role.ADMIN, "SeedAdmin123")).isFalse();

        User completed = userRepository.findByEmailIgnoreCase(invited).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(passwordEncoder.matches("SeedAdmin123", completed.getPasswordHash())).isTrue();
        User untouched = userRepository.findByEmailIgnoreCase(active).orElseThrow();
        assertThat(passwordEncoder.matches("KeepMe12345", untouched.getPasswordHash())).isTrue();
        assertThat(userRepository.findByEmailIgnoreCase(otherRole).orElseThrow().getStatus())
                .isEqualTo(UserStatus.INVITED);
        userRepository.deleteAll(userRepository.findAllById(java.util.List.of(completed.getId(), untouched.getId(),
                userRepository.findByEmailIgnoreCase(otherRole).orElseThrow().getId())));
    }

    @Test
    void runningAgainDoesNotTouchExistingUsers() {
        User before = userRepository.findByEmailIgnoreCase("seed-admin@example.local").orElseThrow();
        long count = userRepository.count();

        runner.run(new DefaultApplicationArguments());

        User after = userRepository.findByEmailIgnoreCase("seed-admin@example.local").orElseThrow();
        assertThat(userRepository.count()).isEqualTo(count);
        assertThat(after.getId()).isEqualTo(before.getId());
        assertThat(after.getPasswordHash()).isEqualTo(before.getPasswordHash());
        assertThat(after.getVersion()).isEqualTo(before.getVersion());
    }
}
