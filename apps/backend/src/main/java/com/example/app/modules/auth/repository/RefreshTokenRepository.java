package com.example.app.modules.auth.repository;

import com.example.app.modules.auth.entity.RefreshToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByFamilyIdAndRevokedAtIsNull(String familyId);

    List<RefreshToken> findByUserIdAndRevokedAtIsNull(Long userId);

    /** The live token of a session, if any (index {@code (family_id, revoked_at)}). Used by the sid check. */
    Optional<RefreshToken> findFirstByFamilyIdAndRevokedAtIsNullAndExpiresAtAfter(String familyId, Instant now);
}
