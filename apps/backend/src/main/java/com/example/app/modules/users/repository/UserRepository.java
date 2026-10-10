package com.example.app.modules.users.repository;

import com.example.app.common.security.Role;
import com.example.app.modules.users.entity.User;
import com.example.app.modules.users.entity.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Internal to the users module: other modules go through the users services. */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRole(Role role);

    long countByRoleAndStatus(Role role, UserStatus status);
}
