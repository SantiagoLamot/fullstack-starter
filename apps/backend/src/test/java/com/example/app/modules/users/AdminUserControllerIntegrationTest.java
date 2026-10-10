package com.example.app.modules.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.app.common.security.Role;
import com.example.app.modules.users.entity.User;
import com.example.app.modules.users.repository.UserRepository;
import com.example.app.support.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class AdminUserControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String URL = "/api/v1/admin/users";
    private static final String URN = "urn:app:problem:";

    @Autowired
    private UserRepository userRepository;

    // ------------------------------------------------------------------ security

    @Test
    void listAndCreateRequireAuthentication() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminRolesGet403() throws Exception {
        for (Role role : new Role[] {Role.USER}) {
            mockMvc.perform(get(URL).with(as(role))).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").value(URN + "forbidden"));
            mockMvc.perform(post(URL).with(as(role)).contentType(MediaType.APPLICATION_JSON)
                            .content(validBody("x-" + UUID.randomUUID() + "@example.test")))
                    .andExpect(status().isForbidden());
        }
    }

    // ------------------------------------------------------------------ create

    @Test
    void createInvitesTheUserAndSendsTheMail() throws Exception {
        String email = "New." + UUID.randomUUID() + "@Example.Test";
        mockMvc.perform(post(URL).with(as(Role.ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.status").value("INVITED"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        assertThat(userRepository.findByEmailIgnoreCase(email)).isPresent();
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly(email.toLowerCase());
        assertThat(captor.getValue().getText()).contains("/activar?token=");
    }

    @Test
    void createRejectsDuplicatedEmailWith409() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@example.test";
        mockMvc.perform(post(URL).with(as(Role.ADMIN)).contentType(MediaType.APPLICATION_JSON)
                .content(validBody(email))).andExpect(status().isCreated());
        mockMvc.perform(post(URL).with(as(Role.ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(email.toUpperCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(URN + "conflict"));
    }

    @Test
    void createValidatesEveryField() throws Exception {
        mockMvc.perform(post(URL).with(as(Role.ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nope\",\"firstName\":\" \",\"lastName\":\"\",\"phone\":\""
                                + "9".repeat(31) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(URN + "validation-error"))
                .andExpect(jsonPath("$.errors", hasSize(5)))
                .andExpect(jsonPath("$.errors[*].field",
                        org.hamcrest.Matchers.containsInAnyOrder("email", "firstName", "lastName", "phone", "role")));
    }

    // ------------------------------------------------------------------ list

    @Test
    void listIsPagedAndFiltersByTextRoleAndStatus() throws Exception {
        String tag = "zq" + UUID.randomUUID().toString().substring(0, 8);
        save(tag + "-a@example.test", "Alba", tag, Role.USER);
        save(tag + "-b@example.test", "Beto", tag, Role.USER);
        User gastro = save(tag + "-c@example.test", "Celia", tag, Role.ADMIN);
        gastro.activate("hash");
        userRepository.save(gastro);

        mockMvc.perform(get(URL).with(as(Role.ADMIN)).param("q", tag.toUpperCase()).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
        mockMvc.perform(get(URL).with(as(Role.ADMIN)).param("q", tag).param("role", "ADMIN"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Celia"));
        mockMvc.perform(get(URL).with(as(Role.ADMIN)).param("q", tag).param("status", "INVITED"))
                .andExpect(jsonPath("$.totalElements").value(2));
        // LIKE wildcards in the text are literals
        mockMvc.perform(get(URL).with(as(Role.ADMIN)).param("q", "%"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listValidatesQueryParameters() throws Exception {
        mockMvc.perform(get(URL).with(as(Role.ADMIN)).param("size", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"));
        mockMvc.perform(get(URL).with(as(Role.ADMIN)).param("page", "-1")).andExpect(status().isBadRequest());
        mockMvc.perform(get(URL).with(as(Role.ADMIN)).param("role", "KING")).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ helpers

    private User save(String email, String firstName, String lastName, Role role) {
        return userRepository.save(User.invite(email, firstName, lastName, null, role));
    }

    private static RequestPostProcessor as(Role role) {
        return jwt().jwt(token -> token.subject("1")).authorities(new SimpleGrantedAuthority(role.authority()));
    }

    private static String validBody(String email) {
        return "{\"email\":\"" + email + "\",\"firstName\":\"Ana\",\"lastName\":\"Gómez\",\"phone\":\"1155551234\","
                + "\"role\":\"USER\"}";
    }
}
