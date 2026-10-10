package com.example.app.modules.users.controller;

import com.example.app.common.security.AdminOnly;
import com.example.app.common.security.CurrentUser;
import com.example.app.modules.users.dto.request.UpdateUserRequest;
import com.example.app.common.security.Role;
import com.example.app.common.web.PageResponse;
import com.example.app.modules.users.entity.UserStatus;
import com.example.app.modules.users.dto.request.CreateUserRequest;
import com.example.app.modules.users.dto.response.UserResponse;
import com.example.app.modules.users.service.UserAdminService;
import com.example.app.modules.users.service.UserSearchCriteria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@Validated
@AdminOnly
public class AdminUserController {

    private final UserAdminService userAdminService;

    public AdminUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public PageResponse<UserResponse> list(
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return userAdminService.search(new UserSearchCriteria(q, role, status), page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userAdminService.create(request);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userAdminService.get(id);
    }

    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userAdminService.update(id, request);
    }

    @PostMapping("/{id}/invitation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendInvitation(@PathVariable Long id) {
        userAdminService.resendInvitation(id);
    }

    @PostMapping("/{id}/disable")
    public UserResponse disable(@PathVariable Long id, @CurrentUser Long adminId) {
        return userAdminService.disable(id, adminId);
    }


    @PostMapping("/{id}/enable")
    public UserResponse enable(@PathVariable Long id) {
        return userAdminService.enable(id);
    }
}
