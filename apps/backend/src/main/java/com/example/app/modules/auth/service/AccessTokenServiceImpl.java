package com.example.app.modules.auth.service;

import com.example.app.common.security.SecurityUtils;
import com.example.app.config.AppProperties;
import com.example.app.config.SecurityConfig;
import com.example.app.modules.users.entity.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenServiceImpl implements AccessTokenService {

    private final JwtEncoder jwtEncoder;
    private final AppProperties properties;
    private final Clock clock;

    public AccessTokenServiceImpl(JwtEncoder jwtEncoder, AppProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public AccessToken issue(User user, String sessionId) {
        Duration ttl = properties.security().jwt().accessTokenTtl();
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.security().jwt().issuer())
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim(SecurityConfig.ROLES_CLAIM, List.of(user.getRole().name()))
                .claim(SecurityUtils.SESSION_ID_CLAIM, Objects.requireNonNull(sessionId, "sessionId"))
                .build();
        String value = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AccessToken(value, ttl.toSeconds());
    }
}
