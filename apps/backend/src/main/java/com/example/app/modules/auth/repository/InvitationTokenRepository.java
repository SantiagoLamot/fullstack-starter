package com.example.app.modules.auth.repository;

import com.example.app.modules.auth.entity.InvitationToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationTokenRepository extends JpaRepository<InvitationToken, Long> {

    Optional<InvitationToken> findByTokenHash(String tokenHash);

    List<InvitationToken> findByUserIdAndUsedAtIsNull(Long userId);
}
