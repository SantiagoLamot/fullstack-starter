package com.example.app.modules.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.example.app.common.security.Role;
import com.example.app.modules.users.entity.User;
import com.example.app.modules.users.entity.UserStatus;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");

    @Test
    void invitedUserActivatesWithPassword() {
        User user = User.invite(" Juan@Mail.com ", " Juan ", "Gómez", null, Role.USER);

        assertThat(user.getEmail()).isEqualTo("juan@mail.com");
        assertThat(user.getFirstName()).isEqualTo("Juan");
        assertThat(user.canLogin(NOW)).isFalse();

        user.activate("hash");

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.canLogin(NOW)).isTrue();
        assertThatIllegalStateException().isThrownBy(() -> user.activate("other"));
    }

    @Test
    void locksAfterMaxFailedAttempts() {
        User user = User.invite("a@b.com", "A", "B", null, Role.ADMIN);
        user.activate("hash");

        user.registerFailedLogin(2, Duration.ofMinutes(15), NOW);
        assertThat(user.isLocked(NOW)).isFalse();
        user.registerFailedLogin(2, Duration.ofMinutes(15), NOW);

        assertThat(user.isLocked(NOW)).isTrue();
        assertThat(user.canLogin(NOW)).isFalse();
        assertThat(user.isLocked(NOW.plus(Duration.ofMinutes(16)))).isFalse();
    }

    @Test
    void disableAndEnable() {
        User user = User.invite("a@b.com", "A", "B", null, Role.ADMIN);
        user.disable();
        assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);

        user.enable();
        assertThat(user.getStatus()).isEqualTo(UserStatus.INVITED);
    }
}
