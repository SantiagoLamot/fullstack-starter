package com.example.app.modules.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.example.app.common.security.Role;
import com.example.app.modules.users.entity.User;
import com.example.app.modules.users.entity.UserStatus;
import com.example.app.modules.users.repository.UserRepository;
import com.example.app.support.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class UserManagementIntegrationTest extends AbstractIntegrationTest {

    private static final String URL = "/api/v1/admin/users";
    private static final String URN = "urn:app:problem:";
    private static final String PASSWORD = "Sup3rSecret-pass";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    // ------------------------------------------------------------------ security

    @Test
    void endpointsRequireAuthenticationAndAdminRole() throws Exception {
        mockMvc.perform(get(URL + "/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(URL + "/1/disable")).andExpect(status().isUnauthorized());
        for (Role role : new Role[] {Role.USER}) {
            mockMvc.perform(get(URL + "/1").with(as(role, 1L))).andExpect(status().isForbidden());
            mockMvc.perform(patch(URL + "/1").with(as(role, 1L)).contentType(MediaType.APPLICATION_JSON)
                    .content("{}")).andExpect(status().isForbidden());
            mockMvc.perform(post(URL + "/1/invitation").with(as(role, 1L))).andExpect(status().isForbidden());
            mockMvc.perform(post(URL + "/1/disable").with(as(role, 1L))).andExpect(status().isForbidden());
            mockMvc.perform(post(URL + "/1/enable").with(as(role, 1L))).andExpect(status().isForbidden());
        }
    }

    // ------------------------------------------------------------------ get / update

    @Test
    void getReturnsTheUserOrNotFound() throws Exception {
        User user = invited("Ana", "Gomez");
        mockMvc.perform(get(URL + "/" + user.getId()).with(as(Role.ADMIN, 1L))).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.phone").value("1155551234"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mockMvc.perform(get(URL + "/999999").with(as(Role.ADMIN, 1L))).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(URN + "not-found"));
    }

    @Test
    void patchUpdatesProfileFieldsButNeverEmailOrRole() throws Exception {
        User user = invited("Ana", "Gomez");
        mockMvc.perform(patch(URL + "/" + user.getId()).with(as(Role.ADMIN, 1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Anita\",\"phone\":\"541144445555\","
                                + "\"email\":\"hacker@example.test\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Anita"))
                .andExpect(jsonPath("$.lastName").value("Gomez"))
                .andExpect(jsonPath("$.phone").value("541144445555"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.email").value(user.getEmail()));
        mockMvc.perform(patch(URL + "/" + user.getId()).with(as(Role.ADMIN, 1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"phone\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").doesNotExist());
    }

    @Test
    void patchValidatesFieldsAndMissingUserIs404() throws Exception {
        User user = invited("Ana", "Gomez");
        mockMvc.perform(patch(URL + "/" + user.getId()).with(as(Role.ADMIN, 1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\" \",\"lastName\":\"" + "x".repeat(101) + "\",\"phone\":\"+54 11 5555\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field",
                        containsInAnyOrder("firstName", "lastName", "phone")));
        for (String phone : new String[] {"1234567", "1234567890123456"}) {
            mockMvc.perform(patch(URL + "/" + user.getId()).with(as(Role.ADMIN, 1L))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"phone\":\"" + phone + "\"}"))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(patch(URL + "/999999").with(as(Role.ADMIN, 1L)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"X\"}")).andExpect(status().isNotFound());
    }

    @Test
    void createRejectsPhonesWithSymbols() throws Exception {
        String email = "phone-" + UUID.randomUUID() + "@example.test";
        mockMvc.perform(post(URL).with(as(Role.ADMIN, 1L)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, "1155551234")))
                .andExpect(status().isCreated());
        mockMvc.perform(post(URL).with(as(Role.ADMIN, 1L)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("x-" + UUID.randomUUID() + "@example.test", "11-5555")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("phone"));
    }

    // ------------------------------------------------------------------ resend invitation

    @Test
    void resendInvitationSendsANewMailForInvitedUsers() throws Exception {
        User user = invited("Ana", "Gomez");
        mockMvc.perform(post(URL + "/" + user.getId() + "/invitation").with(as(Role.ADMIN, 1L)))
                .andExpect(status().isNoContent());
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly(user.getEmail());
        assertThat(captor.getValue().getText()).contains("/activar?token=");
    }

    @Test
    void resendInvitationIs422UnlessInvitedAnd404ForUnknown() throws Exception {
        User active = active("Ana", "Gomez");
        mockMvc.perform(post(URL + "/" + active.getId() + "/invitation").with(as(Role.ADMIN, 1L)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value(URN + "business-rule"));
        mockMvc.perform(post(URL + "/999999/invitation").with(as(Role.ADMIN, 1L)))
                .andExpect(status().isNotFound());
        verifyNoInteractions(mailSender);
    }

    // ------------------------------------------------------------------ disable / enable

    @Test
    void disableRevokesSessionsAndEnableRestoresTheAccount() throws Exception {
        User user = active("Ana", "Gomez");
        String refreshToken = JsonPath.read(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.refreshToken");

        mockMvc.perform(post(URL + "/" + user.getId() + "/disable").with(as(Role.ADMIN, 1L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DISABLED"));
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}")).andExpect(status().isUnauthorized());

        mockMvc.perform(post(URL + "/" + user.getId() + "/enable").with(as(Role.ADMIN, 1L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        assertThat(userRepository.findById(user.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void anAdminCannotDisableThemselves() throws Exception {
        User admin = userRepository.save(User.invite("adm-" + UUID.randomUUID() + "@example.test", "Admin", "Uno",
                null, Role.ADMIN));
        mockMvc.perform(post(URL + "/" + admin.getId() + "/disable").with(as(Role.ADMIN, admin.getId())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value(URN + "business-rule"));
        assertThat(userRepository.findById(admin.getId()).orElseThrow().getStatus())
                .isEqualTo(UserStatus.INVITED);
    }

    @Test
    void disableAndEnableOfUnknownUsersAre404() throws Exception {
        mockMvc.perform(post(URL + "/999999/disable").with(as(Role.ADMIN, 1L))).andExpect(status().isNotFound());
        mockMvc.perform(post(URL + "/999999/enable").with(as(Role.ADMIN, 1L))).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ helpers

    private User invited(String firstName, String lastName) {
        return userRepository.save(User.invite("u-" + UUID.randomUUID() + "@example.test", firstName, lastName,
                "1155551234", Role.USER));
    }

    private User active(String firstName, String lastName) {
        User user = User.invite("u-" + UUID.randomUUID() + "@example.test", firstName, lastName, null, Role.USER);
        user.activate(passwordEncoder.encode(PASSWORD));
        return userRepository.save(user);
    }

    private static RequestPostProcessor as(Role role, Long userId) {
        return jwt().jwt(token -> token.subject(String.valueOf(userId)))
                .authorities(new SimpleGrantedAuthority(role.authority()));
    }

    private static String body(String email, String phone) {
        return "{\"email\":\"" + email + "\",\"firstName\":\"Ana\",\"lastName\":\"Gomez\""
                + ",\"phone\":\"" + phone + "\",\"role\":\"USER\"}";
    }
}
