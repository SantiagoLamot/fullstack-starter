package com.example.app.modules.users.service;

import com.example.app.common.exception.BusinessException;
import com.example.app.common.exception.ConflictException;
import com.example.app.common.exception.NotFoundException;
import com.example.app.common.security.Role;
import com.example.app.common.web.PageResponse;
import com.example.app.modules.auth.service.InvitationService;
import com.example.app.modules.auth.service.RefreshTokenService;
import com.example.app.modules.users.entity.User;
import com.example.app.modules.users.entity.UserStatus;
import com.example.app.modules.users.dto.request.UpdateUserRequest;
import com.example.app.modules.users.dto.request.CreateUserRequest;
import com.example.app.modules.users.dto.response.UserResponse;
import com.example.app.modules.users.mapper.UserMapper;
import com.example.app.modules.users.repository.UserRepository;
import com.example.app.modules.users.repository.UserSpecifications;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    private static final Sort DEFAULT_SORT = Sort.by("lastName", "firstName", "id");

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final InvitationService invitationService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher events;

    public UserAdminServiceImpl(UserRepository userRepository, UserMapper userMapper,
            InvitationService invitationService, PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService, ApplicationEventPublisher events) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.invitationService = invitationService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.events = events;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(UserSearchCriteria criteria, int page, int size) {
        Specification<User> spec = Specification.allOf(
                UserSpecifications.textContains(criteria.text()),
                UserSpecifications.hasRole(criteria.role()),
                UserSpecifications.hasStatus(criteria.status()));
        return PageResponse.from(userRepository.findAll(spec, PageRequest.of(page, size, DEFAULT_SORT)),
                userMapper::toResponse);
    }

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(User.normalizeEmail(request.email()))) {
            throw new ConflictException("Ya existe un usuario con ese email.");
        }
        return userMapper.toResponse(invite(request.email(), request.firstName(), request.lastName(),
                request.phone(), request.role()));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return userMapper.toResponse(find(id));
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = find(id);
        user.updateProfile(request.firstName(), request.lastName(), request.phone());
        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }

    @Override
    @Transactional
    public void resendInvitation(Long id) {
        User user = find(id);
        if (user.getStatus() != UserStatus.INVITED) {
            throw new BusinessException("Solo se puede reenviar la invitación a usuarios pendientes de activar.");
        }
        invitationService.issue(user.getId(), user.getEmail(), user.getFirstName());
    }

    @Override
    @Transactional
    public UserResponse disable(Long id, Long currentAdminId) {
        if (id.equals(currentAdminId)) {
            throw new BusinessException("No podés deshabilitar tu propia cuenta.");
        }
        User user = find(id);
        user.disable();
        refreshTokenService.revokeAllOfUser(user.getId());
        events.publishEvent(new UserDeactivatedEvent(user.getId()));
        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }

    @Override
    @Transactional
    public UserResponse enable(Long id) {
        User user = find(id);
        user.enable();
        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }


    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("El usuario no existe."));
    }

    @Override
    @Transactional
    public boolean bootstrapFirstAdmin(String email, String firstName, String lastName) {
        if (userRepository.existsByRole(Role.ADMIN)
                || userRepository.existsByEmailIgnoreCase(User.normalizeEmail(email))) {
            return false;
        }
        invite(email, firstName, lastName, null, Role.ADMIN);
        return true;
    }

    @Override
    @Transactional
    public boolean createActiveIfAbsent(String email, String firstName, String lastName, Role role,
            String rawPassword) {
        Optional<User> existing = userRepository.findByEmailIgnoreCase(User.normalizeEmail(email));
        if (existing.isPresent()) {
            // An INVITED account without a password (e.g. the ADMIN_BOOTSTRAP_EMAIL invitation) is completed with the
            // seed password; ACTIVE (or disabled) accounts and accounts of another role are never touched.
            User user = existing.get();
            if (user.getStatus() != UserStatus.INVITED || user.getPasswordHash() != null || user.getRole() != role) {
                return false;
            }
            user.activate(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
            return true;
        }
        User user = User.invite(email, firstName, lastName, null, role);
        user.activate(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
        return true;
    }

    private User invite(String email, String firstName, String lastName, String phone, Role role) {
        User user = userRepository.save(User.invite(email, firstName, lastName, phone, role));
        invitationService.issue(user.getId(), user.getEmail(), user.getFirstName());
        return user;
    }
}
