package com.example.app.modules.auth.service;

import com.example.app.modules.auth.dto.response.AuthUserResponse;

public interface AuthService {

    /** Wrong credentials, unknown email, disabled, not yet activated or locked account: same 401. */
    AuthSession login(String email, String password);

    /** Rotates the refresh token (reuse of a revoked one revokes the whole family). */
    AuthSession refresh(String rawRefreshToken);

    /**
     * Revokes the session (family) of the given refresh token. Public on purpose: works with an expired access
     * token. Missing or unknown token: no-op.
     */
    void logout(String rawRefreshToken);

    AuthUserResponse me(Long userId);
}
