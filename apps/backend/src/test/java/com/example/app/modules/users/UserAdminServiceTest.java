package com.example.app.modules.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.app.common.security.Role;
import com.example.app.modules.auth.service.InvitationService;
import com.example.app.modules.auth.service.RefreshTokenService;
import com.example.app.modules.users.entity.User;
import com.example.app.modules.users.mapper.UserMapper;
import com.example.app.modules.users.repository.UserRepository;
import com.example.app.modules.users.service.UserAdminService;
import com.example.app.modules.users.service.UserAdminServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserAdminServiceTest {

    private final UserRepository repository = mock(UserRepository.class);
    private final InvitationService invitations = mock(InvitationService.class);
    private final UserAdminService service =
            new UserAdminServiceImpl(repository, mock(UserMapper.class), invitations,
                    mock(PasswordEncoder.class), mock(RefreshTokenService.class),
                    mock(ApplicationEventPublisher.class));

    @Test
    void bootstrapDoesNothingWhenAnAdminAlreadyExists() {
        when(repository.existsByRole(Role.ADMIN)).thenReturn(true);

        assertThat(service.bootstrapFirstAdmin("a@example.test", "Admin", "App")).isFalse();

        verify(repository, never()).save(any());
        verify(invitations, never()).issue(any(), anyString(), anyString());
    }

    @Test
    void bootstrapCreatesAnInvitedAdminAndSendsTheInvitation() {
        when(repository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("a@example.test")).thenReturn(false);
        when(repository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        assertThat(service.bootstrapFirstAdmin("A@example.test", "Admin", "App")).isTrue();

        verify(repository).save(Mockito.argThat(user -> user.getRole() == Role.ADMIN
                && user.getPasswordHash() == null && user.getEmail().equals("a@example.test")));
        verify(invitations).issue(any(), Mockito.eq("a@example.test"), Mockito.eq("Admin"));
    }
}
