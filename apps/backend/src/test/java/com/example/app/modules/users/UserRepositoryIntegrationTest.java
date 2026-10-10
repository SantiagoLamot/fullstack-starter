package com.example.app.modules.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.app.common.security.Role;
import com.example.app.modules.users.entity.User;
import com.example.app.modules.users.entity.UserStatus;
import com.example.app.modules.users.repository.UserRepository;
import com.example.app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/** Hibernate creates the {@code users} table on MySQL and the repository round-trips entities. */
@Transactional
class UserRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndReadsUserWithAuditColumns() {
        User saved = userRepository.saveAndFlush(
                User.invite("  Ana.Perez@Example.com ", "Ana", "Pérez", " ", Role.USER));

        User found = userRepository.findByEmailIgnoreCase("ANA.PEREZ@example.com").orElseThrow();

        assertThat(found.getId()).isNotNull().isEqualTo(saved.getId());
        assertThat(found.getEmail()).isEqualTo("ana.perez@example.com");
        assertThat(found.getLastName()).isEqualTo("Pérez");
        assertThat(found.getPhone()).isNull();
        assertThat(found.getRole()).isEqualTo(Role.USER);
        assertThat(found.getStatus()).isEqualTo(UserStatus.INVITED);
        assertThat(found.getPasswordHash()).isNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.getCreatedBy()).isNull();
        assertThat(userRepository.existsByEmailIgnoreCase("ana.perez@EXAMPLE.com")).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCase("nobody@example.com")).isFalse();
    }

    @Test
    void emailIsUnique() {
        userRepository.saveAndFlush(User.invite("dup@example.com", "Uno", "Dos", null, Role.ADMIN));

        assertThatThrownBy(() -> userRepository.saveAndFlush(
                User.invite("DUP@example.com", "Tres", "Cuatro", null, Role.ADMIN)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
