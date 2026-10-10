package com.example.app.modules.auth.controller;

import com.example.app.common.exception.UnauthorizedException;
import com.example.app.common.security.Authenticated;
import com.example.app.common.security.CurrentUser;
import com.example.app.common.security.SecurityUtils;
import com.example.app.modules.auth.dto.request.ChangePasswordRequest;
import com.example.app.modules.auth.dto.response.AuthUserResponse;
import com.example.app.modules.auth.service.AuthService;
import com.example.app.modules.auth.service.PasswordChangeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints of an already authenticated session (any role). User and session always come from the JWT. */
@RestController
@RequestMapping("/api/v1/auth")
@Validated
@Authenticated
public class SessionController {

    private final AuthService authService;
    private final PasswordChangeService passwordChangeService;

    public SessionController(AuthService authService, PasswordChangeService passwordChangeService) {
        this.authService = authService;
        this.passwordChangeService = passwordChangeService;
    }

    @GetMapping("/me")
    public AuthUserResponse me(@CurrentUser Long userId) {
        return authService.me(userId);
    }

    /** Keeps the current session and closes every other one. */
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@CurrentUser Long userId, @Valid @RequestBody ChangePasswordRequest request) {
        String sessionId = SecurityUtils.currentSessionId()
                .orElseThrow(() -> new UnauthorizedException("Se requiere un token de acceso válido."));
        passwordChangeService.change(userId, sessionId, request.currentPassword(), request.newPassword());
    }
}
