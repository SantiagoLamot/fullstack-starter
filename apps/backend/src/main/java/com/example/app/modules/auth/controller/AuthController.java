package com.example.app.modules.auth.controller;

import com.example.app.common.exception.UnauthorizedException;
import com.example.app.common.security.PublicEndpoint;
import com.example.app.modules.auth.dto.request.AcceptInvitationRequest;
import com.example.app.modules.auth.dto.request.LoginRequest;
import com.example.app.modules.auth.dto.request.LogoutRequest;
import com.example.app.modules.auth.dto.request.PasswordResetConfirmRequest;
import com.example.app.modules.auth.dto.request.PasswordResetRequest;
import com.example.app.modules.auth.dto.request.RefreshRequest;
import com.example.app.modules.auth.dto.response.TokenResponse;
import com.example.app.modules.auth.service.AuthService;
import com.example.app.modules.auth.service.AuthSession;
import com.example.app.modules.auth.service.InvitationService;
import com.example.app.modules.auth.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public authentication endpoints (whitelisted in SecurityConfig). The Authorization header is ignored here
 * (see {@code PublicPostBearerTokenResolver}).
 */
@RestController
@RequestMapping("/api/v1/auth")
@Validated
@PublicEndpoint
public class AuthController {

    private final AuthService authService;
    private final InvitationService invitationService;
    private final RefreshCookies refreshCookies;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, InvitationService invitationService,
            RefreshCookies refreshCookies, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.invitationService = invitationService;
        this.refreshCookies = refreshCookies;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest http) {
        return toResponse(authService.login(request.email(), request.password()), http);
    }

    /** Web: refresh token from the cookie (header X-Requested-With required). Mobile: from the body. */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody(required = false) RefreshRequest body,
            HttpServletRequest http) {
        String token = refreshCookies.isWebClient(http)
                ? refreshCookies.read(http)
                : body == null ? null : body.refreshToken();
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("La sesión es inválida o expiró. Iniciá sesión de nuevo.");
        }
        return toResponse(authService.refresh(token), http);
    }

    @PostMapping("/invitations/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptInvitation(@Valid @RequestBody AcceptInvitationRequest request) {
        invitationService.accept(request.token(), request.password());
    }

    /**
     * Revokes the session identified by the refresh token (web: cookie + X-Requested-With; mobile: body). Works
     * with an expired access token. Always 204; web always clears the cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody(required = false) LogoutRequest body,
            HttpServletRequest http) {
        boolean web = refreshCookies.isWebClient(http);
        authService.logout(web ? refreshCookies.read(http) : body == null ? null : body.refreshToken());
        ResponseEntity.HeadersBuilder<?> response = ResponseEntity.noContent();
        if (web) {
            response.header(HttpHeaders.SET_COOKIE, refreshCookies.clear());
        }
        return response.build();
    }

    /** Always 204 (no account enumeration); 429 when the client IP asked too many times. */
    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request, HttpServletRequest http) {
        passwordResetService.request(request.email(), http.getRemoteAddr());
    }

    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request,
            HttpServletRequest http) {
        passwordResetService.confirm(request.token(), request.newPassword(), http.getRemoteAddr());
    }

    private ResponseEntity<TokenResponse> toResponse(AuthSession session, HttpServletRequest http) {
        boolean web = refreshCookies.isWebClient(http);
        TokenResponse body = new TokenResponse(session.accessToken(), TokenResponse.BEARER,
                session.expiresInSeconds(), web ? null : session.refreshToken(), session.user());
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (web) {
            builder.header(HttpHeaders.SET_COOKIE, refreshCookies.issue(session.refreshToken()));
        }
        return builder.body(body);
    }
}
