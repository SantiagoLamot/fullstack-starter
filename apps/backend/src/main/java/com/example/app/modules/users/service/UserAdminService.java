package com.example.app.modules.users.service;

import com.example.app.common.security.Role;
import com.example.app.common.web.PageResponse;
import com.example.app.modules.users.dto.request.CreateUserRequest;
import com.example.app.modules.users.dto.request.UpdateUserRequest;
import com.example.app.modules.users.dto.response.UserResponse;

/** User management for administrators. */
public interface UserAdminService {

    PageResponse<UserResponse> search(UserSearchCriteria criteria, int page, int size);

    /** 404 if the user does not exist. */
    UserResponse get(Long id);

    /** Creates an INVITED user and sends the invitation. 409 if the email is already registered. */
    UserResponse create(CreateUserRequest request);

    /** Partial update of name and phone (email and role are never edited). */
    UserResponse update(Long id, UpdateUserRequest request);

    /** Invalidates the previous invitation and sends a new one. 422 unless the user is INVITED. */
    void resendInvitation(Long id);

    /** Disables the account and revokes every session. 422 if the admin disables themselves. */
    UserResponse disable(Long id, Long currentAdminId);

    UserResponse enable(Long id);

    /** Creates the first ADMIN (invited) unless an ADMIN already exists. Returns whether it was created. */
    boolean bootstrapFirstAdmin(String email, String firstName, String lastName);

    /** Creates an already ACTIVE user with the given password (hashed here) unless the email exists. If it exists as an INVITED
     * user of the same role without a password, it is completed and activated instead. ACTIVE users are never touched.
     * Returns true when a user was created or activated. Dev seed only. */
    boolean createActiveIfAbsent(String email, String firstName, String lastName, Role role, String rawPassword);
}
