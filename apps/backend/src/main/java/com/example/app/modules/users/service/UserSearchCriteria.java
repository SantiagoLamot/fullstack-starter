package com.example.app.modules.users.service;

import com.example.app.common.security.Role;
import com.example.app.modules.users.entity.UserStatus;

/** Optional filters of the admin user listing. */
public record UserSearchCriteria(String text, Role role, UserStatus status) {
}
