package com.example.app.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.example.app.common.security.Role;
import com.example.app.modules.users.entity.User;
import com.example.app.modules.users.repository.UserRepository;
import com.example.app.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class AuthIntegrationTest extends AbstractIntegrationTest {

    private static final String URN = "urn:app:problem:";
    private static final String PASSWORD = "Sup3rSecret-pass";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ------------------------------------------------------------------ login

    @Test
    void mobileLoginReturnsTokensInBodyAndAccessTokenWorks() throws Exception {
        User user = activeUser(Role.USER);
        MvcResult result = login(user.getEmail(), PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(user.getEmail()))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn();

        String access = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value(user.getEmail()));
    }

    @Test
    void webLoginSetsHttpOnlyCookieAndOmitsRefreshTokenFromBody() throws Exception {
        User user = activeUser(Role.ADMIN);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(user.getEmail(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().secure("refresh_token", true))
                .andExpect(cookie().path("refresh_token", "/api/v1/auth"))
                .andReturn();
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("SameSite=Strict");
    }

    @Test
    void loginValidatesTheBody() throws Exception {
        login("not-an-email", "").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(URN + "validation-error"))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[1].field").value("password"));
    }

    @Test
    void wrongPasswordAndUnknownEmailAndDisabledAccountAreIndistinguishable() throws Exception {
        User active = activeUser(Role.USER);
        User disabled = activeUser(Role.USER);
        disabled.disable();
        userRepository.save(disabled);

        String wrongPassword = body(login(active.getEmail(), "Wrong-pass-123").andExpect(status().isUnauthorized()));
        String unknown = body(login("nobody-" + UUID.randomUUID() + "@example.test", PASSWORD)
                .andExpect(status().isUnauthorized()));
        String disabledBody = body(login(disabled.getEmail(), PASSWORD).andExpect(status().isUnauthorized()));

        assertThat(JsonPath.<String>read(wrongPassword, "$.detail"))
                .isEqualTo(JsonPath.<String>read(unknown, "$.detail"))
                .isEqualTo(JsonPath.<String>read(disabledBody, "$.detail"));
        assertThat(JsonPath.<String>read(unknown, "$.type")).isEqualTo(URN + "unauthorized");
    }

    @Test
    void accountIsLockedAfterTooManyFailedAttempts() throws Exception {
        User user = activeUser(Role.USER);
        for (int i = 0; i < 5; i++) {
            login(user.getEmail(), "Wrong-pass-123").andExpect(status().isUnauthorized());
        }
        // even the right password is rejected (same generic 401) while locked
        login(user.getEmail(), PASSWORD).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ refresh

    @Test
    void refreshRotatesTheTokenAndReuseRevokesTheWholeFamily() throws Exception {
        User user = activeUser(Role.USER);
        String first = refreshTokenOf(login(user.getEmail(), PASSWORD).andReturn());

        MvcResult rotated = refresh(first).andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        String second = refreshTokenOf(rotated);
        assertThat(second).isNotEqualTo(first);

        // reusing the already rotated token is treated as theft...
        refresh(first).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(URN + "unauthorized"));
        // ...and kills the legitimate descendant too
        refresh(second).andExpect(status().isUnauthorized());
    }

    @Test
    void webRefreshUsesTheCookieAndSetsANewOne() throws Exception {
        User user = activeUser(Role.ADMIN);
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(user.getEmail(), PASSWORD)))
                .andReturn();
        Cookie cookie = login.getResponse().getCookie("refresh_token");

        mockMvc.perform(post("/api/v1/auth/refresh").header("X-Requested-With", "XMLHttpRequest").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")));
        // without the custom header the cookie is ignored (CSRF defense) and there is no body token
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(login.getResponse().getCookie("refresh_token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshValidatesTheBodyAndRejectsUnknownTokens() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("refreshToken"));
        refresh("does-not-exist").andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshIsRejectedOnceTheAccountIsDisabled() throws Exception {
        User user = activeUser(Role.USER);
        String token = refreshTokenOf(login(user.getEmail(), PASSWORD).andReturn());
        user.disable();
        userRepository.save(user);
        refresh(token).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ logout / me

    @Test
    void meRequiresAuthenticationButLogoutIsPublicAndIdempotent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"unknown-token\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheRefreshTokenFamily() throws Exception {
        User user = activeUser(Role.ADMIN);
        MvcResult login = login(user.getEmail(), PASSWORD).andReturn();
        String access = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        String refreshToken = refreshTokenOf(login);

        mockMvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());
        refresh(refreshToken).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ invitation

    @Test
    void invitationAcceptActivatesTheAccountAndTokenIsSingleUse() throws Exception {
        String token = inviteThroughAdmin("accept-" + UUID.randomUUID() + "@example.test");

        acceptInvitation(token, PASSWORD).andExpect(status().isNoContent());
        acceptInvitation(token, PASSWORD).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value(URN + "business-rule"));
    }

    @Test
    void acceptedInvitationLetsTheUserLogIn() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@example.test";
        String token = inviteThroughAdmin(email);
        login(email, PASSWORD).andExpect(status().isUnauthorized()); // not active yet
        acceptInvitation(token, PASSWORD).andExpect(status().isNoContent());
        login(email, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void invitationAcceptValidatesBodyAndRejectsUnknownTokens() throws Exception {
        acceptInvitation(" ", "short").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", org.hamcrest.Matchers.hasItems("token", "password")));
        acceptInvitation("x".repeat(20), "allletters-only").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
        acceptInvitation("unknown-token", PASSWORD).andExpect(status().isUnprocessableContent());
    }

    @Test
    void resendingAnInvitationInvalidatesThePreviousOne() throws Exception {
        String email = "resend-" + UUID.randomUUID() + "@example.test";
        String first = inviteThroughAdmin(email);
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        invitationService.issue(user.getId(), user.getEmail(), user.getFirstName());
        String second = lastInvitationToken();

        acceptInvitation(first, PASSWORD).andExpect(status().isUnprocessableContent());
        acceptInvitation(second, PASSWORD).andExpect(status().isNoContent());
        assertThat(first).isNotEqualTo(second);
    }

    // ------------------------------------------------------------------ helpers

    @Autowired
    private com.example.app.modules.auth.service.InvitationService invitationService;

    private User activeUser(Role role) {
        User user = User.invite("user-" + UUID.randomUUID() + "@example.test", "Test", "User", null, role);
        user.activate(passwordEncoder.encode(PASSWORD));
        return userRepository.save(user);
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials(email, password)));
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + token + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions acceptInvitation(String token, String password)
            throws Exception {
        return mockMvc.perform(post("/api/v1/auth/invitations/accept").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"password\":\"" + password + "\"}"));
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static String refreshTokenOf(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.refreshToken");
    }

    private static String body(org.springframework.test.web.servlet.ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString();
    }

    /** Creates the user as an admin would and returns the raw token that went into the mail. */
    private String inviteThroughAdmin(String email) throws Exception {
        MockHttpServletRequestBuilder request = post("/api/v1/admin/users")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                        .jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                Role.ADMIN.authority())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"firstName\":\"Ana\",\"lastName\":\"Gómez\","
                        + "\"role\":\"USER\"}");
        mockMvc.perform(request).andExpect(status().isCreated());
        return lastInvitationToken();
    }

    private String lastInvitationToken() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, atLeastOnce()).send(captor.capture());
        String text = captor.getValue().getText();
        Matcher matcher = Pattern.compile("token=([\\w-]+)").matcher(text);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
