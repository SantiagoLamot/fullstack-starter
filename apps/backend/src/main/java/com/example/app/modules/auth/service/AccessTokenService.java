package com.example.app.modules.auth.service;

import com.example.app.modules.users.entity.User;

public interface AccessTokenService {

    /** Signs a short-lived JWT: sub = user id, roles = [role], sid = session (refresh family) id. */
    AccessToken issue(User user, String sessionId);
}
