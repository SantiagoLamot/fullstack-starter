package com.example.app.modules.auth.service;

import com.example.app.common.exception.UnauthorizedException;
import com.example.app.modules.auth.dto.response.AuthUserResponse;
import com.example.app.modules.auth.mapper.AuthUserMapper;
import com.example.app.modules.users.entity.User;
import com.example.app.modules.users.entity.UserStatus;
import com.example.app.modules.users.service.UserAccountService;
import java.time.Clock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Deliberately not {@code @Transactional}: a failed login must still persist the attempt counter, and each
 * collaborator opens its own transaction.
 */
@Service
public class AuthServiceImpl implements AuthService {

    static final String INVALID_CREDENTIALS = "Email o contraseña incorrectos.";

    private final UserAccountService userAccountService;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;
    private final AuthUserMapper authUserMapper;
    private final Clock clock;
    /** Compared against when the email is unknown, so both paths cost the same (no user enumeration by timing). */
    private final String dummyHash;

    public AuthServiceImpl(UserAccountService userAccountService, PasswordEncoder passwordEncoder,
            AccessTokenService accessTokenService, RefreshTokenService refreshTokenService,
            AuthUserMapper authUserMapper, Clock clock) {
        this.userAccountService = userAccountService;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.authUserMapper = authUserMapper;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Override
    public AuthSession login(String email, String password) {
        User user = userAccountService.findByEmail(email).orElse(null);
        if (user == null || !user.canLogin(clock.instant())) {
            passwordEncoder.matches(password, dummyHash);
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            userAccountService.registerFailedLogin(user.getId());
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        userAccountService.registerSuccessfulLogin(user.getId());
        RefreshTokenService.IssuedRefreshToken issued = refreshTokenService.startFamily(user.getId());
        return session(user, issued.familyId(), issued.rawToken());
    }

    @Override
    public AuthSession refresh(String rawRefreshToken) {
        RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(rawRefreshToken);
        User user = userAccountService.findById(rotated.userId()).orElse(null);
        if (user == null || !user.canLogin(clock.instant())) {
            refreshTokenService.revokeAllOfUser(rotated.userId());
            throw new UnauthorizedException(RefreshTokenServiceImpl.INVALID_SESSION);
        }
        return session(user, rotated.familyId(), rotated.newToken());
    }

    @Override
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revokeFamilyOf(rawRefreshToken);
        }
    }

    @Override
    public AuthUserResponse me(Long userId) {
        return userAccountService.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .map(authUserMapper::toResponse)
                .orElseThrow(() -> new UnauthorizedException(RefreshTokenServiceImpl.INVALID_SESSION));
    }

    private AuthSession session(User user, String sessionId, String refreshToken) {
        AccessToken access = accessTokenService.issue(user, sessionId);
        return new AuthSession(access.value(), access.expiresInSeconds(), refreshToken,
                authUserMapper.toResponse(user));
    }
}
