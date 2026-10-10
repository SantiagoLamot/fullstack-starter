package com.example.app.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.example.app.common.security.Role;
import com.example.app.config.AppProperties;
import com.example.app.modules.auth.entity.PasswordResetToken;
import com.example.app.modules.auth.repository.PasswordResetTokenRepository;
import com.example.app.modules.auth.service.OpaqueTokens;
import com.example.app.modules.users.entity.User;
import com.example.app.modules.users.repository.UserRepository;
import com.example.app.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Password reset, password change, public logout and the per-request session check (spec password-auth,
 * ADR 0009). The rate limiter is shared by the whole context: every test uses its own client IP and emails.
 */
class PasswordAuthIntegrationTest extends AbstractIntegrationTest {

    private static final String URN = "urn:app:problem:";
    private static final String PASSWORD = "Sup3rSecret-pass";
    private static final String NEW_PASSWORD = "N3w-password-ok";
    private static final String WEB = "X-Requested-With";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;
    @Autowired
    private OpaqueTokens opaqueTokens;
    @Autowired
    private JwtEncoder jwtEncoder;
    @Autowired
    private AppProperties properties;

    // ------------------------------------------------------------------ reset: request

    @Test
    void resetRequestAlwaysAnswers204ButOnlyActiveAccountsGetTheMail() throws Exception {
        String ip = randomIp();
        User active = activeUser(Role.USER);
        User invited = userRepository.save(User.invite(uniqueEmail(), "Inv", "Itado", null, Role.USER));
        User disabled = activeUser(Role.USER);
        disabled.disable();
        userRepository.save(disabled);
        String unknown = uniqueEmail();

        for (String email : List.of(unknown, invited.getEmail(), disabled.getEmail(),
                active.getEmail())) {
            requestReset(email, ip).andExpect(status().isNoContent());
        }

        String text = lastMailTo(active.getEmail()).getText();
        assertThat(text).contains("/restablecer?token=").contains("vence en 60 minutos")
                .startsWith("Hola Test,");
        assertThat(lastMailTo(active.getEmail()).getSubject())
                .isEqualTo("Restablecé tu contraseña de App");
        for (String email : List.of(unknown, invited.getEmail(), disabled.getEmail())) {
            verify(mailSender, after(300).never()).send(argThat((SimpleMailMessage m) -> isTo(m, email)));
        }
    }

    @Test
    void resetRequestValidatesTheEmail() throws Exception {
        requestReset("not-an-email", randomIp()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void aNewRequestInvalidatesThePreviousToken() throws Exception {
        String ip = randomIp();
        User user = activeUser(Role.USER);
        requestReset(user.getEmail(), ip).andExpect(status().isNoContent());
        String first = tokenOf(waitForMails(user.getEmail(), 1));
        requestReset(user.getEmail(), ip).andExpect(status().isNoContent());
        String second = tokenOf(waitForMails(user.getEmail(), 2));
        assertThat(second).isNotEqualTo(first);

        confirm(first, NEW_PASSWORD, ip).andExpect(status().isUnprocessableContent());
        confirm(second, NEW_PASSWORD, ip).andExpect(status().isNoContent());
    }

    @Test
    void theFourthRequestOfTheSameEmailInAnHourSendsNothingButStillAnswers204() throws Exception {
        String ip = randomIp();
        User user = activeUser(Role.ADMIN);
        for (int i = 0; i < 3; i++) {
            requestReset(user.getEmail(), ip).andExpect(status().isNoContent());
        }
        waitForMails(user.getEmail(), 3);
        requestReset(user.getEmail(), ip).andExpect(status().isNoContent());
        verify(mailSender, after(500).times(3)).send(argThat((SimpleMailMessage m) -> isTo(m, user.getEmail())));
    }

    @Test
    void theSixthRequestFromTheSameIpIsRateLimited() throws Exception {
        String ip = randomIp();
        for (int i = 0; i < 5; i++) {
            requestReset(uniqueEmail(), ip).andExpect(status().isNoContent());
        }
        requestReset(uniqueEmail(), ip).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value(URN + "too-many-requests"))
                .andExpect(jsonPath("$.title").value("Demasiados intentos"))
                .andExpect(jsonPath("$.detail")
                        .value("Hiciste demasiados intentos. Esperá unos minutos y volvé a probar."));
        // another IP is not affected
        requestReset(uniqueEmail(), randomIp()).andExpect(status().isNoContent());
    }

    // ------------------------------------------------------------------ reset: confirm

    @Test
    void confirmChangesThePasswordUnlocksTheAccountAndRevokesEverySession() throws Exception {
        String ip = randomIp();
        User user = activeUser(Role.USER);
        String access = accessTokenOf(login(user.getEmail(), PASSWORD).andExpect(status().isOk()).andReturn());
        for (int i = 0; i < 5; i++) {
            login(user.getEmail(), "Wrong-pass-123").andExpect(status().isUnauthorized());
        }
        login(user.getEmail(), PASSWORD).andExpect(status().isUnauthorized()); // locked

        requestReset(user.getEmail(), ip).andExpect(status().isNoContent());
        String token = tokenOf(waitForMails(user.getEmail(), 1));
        confirm(token, NEW_PASSWORD, ip).andExpect(status().isNoContent());

        me(access).andExpect(status().isUnauthorized());
        login(user.getEmail(), PASSWORD).andExpect(status().isUnauthorized());
        login(user.getEmail(), NEW_PASSWORD).andExpect(status().isOk());
        // single use
        confirm(token, "Another-pass-123", ip).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value(URN + "business-rule"))
                .andExpect(jsonPath("$.detail").value("El enlace es inválido o ya venció. Pedí uno nuevo."));
    }

    @Test
    void expiredOrUnknownTokensAreRejected() throws Exception {
        String ip = randomIp();
        User user = activeUser(Role.USER);
        String raw = opaqueTokens.generate();
        resetTokenRepository.save(new PasswordResetToken(user.getId(), opaqueTokens.hash(raw),
                Instant.now().minus(1, ChronoUnit.MINUTES)));

        confirm(raw, NEW_PASSWORD, ip).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("El enlace es inválido o ya venció. Pedí uno nuevo."));
        confirm("does-not-exist", NEW_PASSWORD, ip).andExpect(status().isUnprocessableContent());
        login(user.getEmail(), PASSWORD).andExpect(status().isOk());
    }

    @Test
    void anAccountDisabledBetweenRequestAndConfirmIsRejectedAndTheTokenIsBurnt() throws Exception {
        String ip = randomIp();
        User user = activeUser(Role.USER);
        requestReset(user.getEmail(), ip).andExpect(status().isNoContent());
        String token = tokenOf(waitForMails(user.getEmail(), 1));
        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        reloaded.disable();
        userRepository.save(reloaded);

        confirm(token, NEW_PASSWORD, ip).andExpect(status().isUnprocessableContent());
        assertThat(resetTokenRepository.findByTokenHash(opaqueTokens.hash(token)).orElseThrow().getUsedAt())
                .isNotNull();
    }

    @Test
    void confirmValidatesTheNewPassword() throws Exception {
        confirm("some-token", "short", randomIp()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("newPassword"));
        confirm("some-token", "onlyletters-no-digits", randomIp()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("newPassword"))
                .andExpect(jsonPath("$.errors[0].message").value("debe incluir al menos una letra y un número"));
    }

    @Test
    void passwordsOverBcrypts72BytesAreA400EvenWith72Characters() throws Exception {
        String accented = "ñ".repeat(70) + "a1"; // 72 characters, 142 UTF-8 bytes
        String message = "es demasiado larga: las letras con tilde y la ñ ocupan el doble, acortala un poco";
        confirm("some-token", accented, randomIp()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("newPassword"))
                .andExpect(jsonPath("$.errors[0].message").value(message));
        mockMvc.perform(post("/api/v1/auth/invitations/accept").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"some-token\",\"password\":\"" + accented + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
        User user = activeUser(Role.USER);
        String access = accessTokenOf(login(user.getEmail(), PASSWORD).andReturn());
        changePassword(access, accented, NEW_PASSWORD).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("currentPassword"));
        changePassword(access, PASSWORD, accented).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("newPassword"));
        login(user.getEmail(), accented).andExpect(status().isBadRequest());
        // 35 x ñ (70 bytes) + "a1" = exactly 72 bytes: still accepted
        String limit = "ñ".repeat(35) + "a1";
        changePassword(access, PASSWORD, limit).andExpect(status().isNoContent());
        login(user.getEmail(), limit).andExpect(status().isOk());
    }

    @Test
    void twoConcurrentConfirmsOfTheSameTokenSucceedOnlyOnce() throws Exception {
        String ip = randomIp();
        User user = activeUser(Role.USER);
        requestReset(user.getEmail(), ip).andExpect(status().isNoContent());
        String token = tokenOf(waitForMails(user.getEmail(), 1));

        int threads = 4;
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        List<java.util.concurrent.Future<Integer>> results = new java.util.ArrayList<>();
        for (int i = 0; i < threads; i++) {
            String password = NEW_PASSWORD + i;
            results.add(pool.submit(() -> {
                start.await();
                return confirm(token, password, ip).andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();
        List<Integer> statuses = new java.util.ArrayList<>();
        for (java.util.concurrent.Future<Integer> result : results) {
            statuses.add(result.get(30, java.util.concurrent.TimeUnit.SECONDS));
        }
        pool.shutdown();
        assertThat(statuses).containsOnly(204, 422);
        assertThat(statuses.stream().filter(s -> s == 204).count()).isEqualTo(1);
    }

    // ------------------------------------------------------------------ change password

    @Test
    void changeKeepsTheCurrentSessionAndClosesTheOthers() throws Exception {
        User user = activeUser(Role.USER);
        MvcResult current = login(user.getEmail(), PASSWORD).andReturn();
        MvcResult other = login(user.getEmail(), PASSWORD).andReturn();
        String currentAccess = accessTokenOf(current);
        String otherAccess = accessTokenOf(other);
        me(otherAccess).andExpect(status().isOk());

        changePassword(currentAccess, PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

        me(currentAccess).andExpect(status().isOk());
        me(otherAccess).andExpect(status().isUnauthorized());
        refresh(refreshTokenOf(other)).andExpect(status().isUnauthorized());
        refresh(refreshTokenOf(current)).andExpect(status().isOk());
        login(user.getEmail(), NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void wrongCurrentPasswordIs422WithoutCountingFailedLoginsAndTheSixthFailureIs429() throws Exception {
        User user = activeUser(Role.ADMIN);
        String access = accessTokenOf(login(user.getEmail(), PASSWORD).andReturn());

        changePassword(access, PASSWORD, PASSWORD).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("La contraseña nueva tiene que ser distinta de la actual."))
                .andExpect(jsonPath("$.type").value(URN + "business-rule"))
                .andExpect(jsonPath("$.errors[0].field").value("newPassword"))
                .andExpect(jsonPath("$.errors[0].message").value("La contraseña nueva tiene que ser distinta de la actual."));
        for (int i = 0; i < 5; i++) {
            changePassword(access, "Wrong-pass-123", NEW_PASSWORD).andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.detail").value("La contraseña actual no es correcta."))
                    .andExpect(jsonPath("$.errors[0].field").value("currentPassword"));
        }
        assertThat(userRepository.findById(user.getId()).orElseThrow().getFailedLoginAttempts()).isZero();
        changePassword(access, PASSWORD, NEW_PASSWORD).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value(URN + "too-many-requests"));
        login(user.getEmail(), PASSWORD).andExpect(status().isOk());
    }

    @Test
    void changeWorksForEveryRoleAndRequiresAuthentication() throws Exception {
        for (Role role : Arrays.asList(Role.USER, Role.ADMIN)) {
            User user = activeUser(role);
            String access = accessTokenOf(login(user.getEmail(), PASSWORD).andReturn());
            changePassword(access, PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());
        }
        mockMvc.perform(put("/api/v1/auth/password").contentType(MediaType.APPLICATION_JSON)
                        .content(changeBody(PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changeValidatesTheBody() throws Exception {
        User user = activeUser(Role.USER);
        String access = accessTokenOf(login(user.getEmail(), PASSWORD).andReturn());
        changePassword(access, "", "short").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field",
                        org.hamcrest.Matchers.hasItems("currentPassword", "newPassword")));
    }

    // ------------------------------------------------------------------ logout and session check

    @Test
    void webLogoutWithAnExpiredAccessTokenRevokesTheSessionAndClearsTheCookie() throws Exception {
        User user = activeUser(Role.ADMIN);
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login").header(WEB, "XMLHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON).content(credentials(user.getEmail(), PASSWORD)))
                .andExpect(status().isOk()).andReturn();
        String access = accessTokenOf(login);
        Cookie cookie = login.getResponse().getCookie("refresh_token");
        me(access).andExpect(status().isOk()); // warms the session cache

        mockMvc.perform(post("/api/v1/auth/logout").header(WEB, "XMLHttpRequest").cookie(cookie)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken(user.getId())))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        mockMvc.perform(post("/api/v1/auth/refresh").header(WEB, "XMLHttpRequest").cookie(cookie))
                .andExpect(status().isUnauthorized());
        // the access token of that session is not expired yet, but the session is gone (no TTL wait)
        me(access).andExpect(status().isUnauthorized());
    }

    @Test
    void mobileLogoutRevokesOnlyThatSession() throws Exception {
        User user = activeUser(Role.USER);
        MvcResult first = login(user.getEmail(), PASSWORD).andReturn();
        MvcResult second = login(user.getEmail(), PASSWORD).andReturn();

        mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshTokenOf(first) + "\"}"))
                .andExpect(status().isNoContent());

        me(accessTokenOf(first)).andExpect(status().isUnauthorized());
        me(accessTokenOf(second)).andExpect(status().isOk());
    }

    @Test
    void disablingAUserCutsItsAccessTokenOnTheNextRequest() throws Exception {
        User user = activeUser(Role.USER);
        String access = accessTokenOf(login(user.getEmail(), PASSWORD).andReturn());
        me(access).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/" + user.getId() + "/disable")
                        .with(jwt().jwt(j -> j.subject("999999"))
                                .authorities(new SimpleGrantedAuthority(Role.ADMIN.authority()))))
                .andExpect(status().isOk());

        me(access).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ helpers

    private User activeUser(Role role) {
        User user = User.invite(uniqueEmail(), "Test", "User", null, role);
        user.activate(passwordEncoder.encode(PASSWORD));
        return userRepository.save(user);
    }

    private static String uniqueEmail() {
        return "pwd-" + UUID.randomUUID() + "@example.test";
    }

    private static String randomIp() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return "10." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(1, 255);
    }

    private static RequestPostProcessor from(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private ResultActions requestReset(String email, String ip) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/password-reset/request").with(from(ip))
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\"}"));
    }

    private ResultActions confirm(String token, String password, String ip) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/password-reset/confirm").with(from(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + password + "\"}"));
    }

    private ResultActions changePassword(String access, String current, String next) throws Exception {
        return mockMvc.perform(put("/api/v1/auth/password").header(HttpHeaders.AUTHORIZATION, "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON).content(changeBody(current, next)));
    }

    private static String changeBody(String current, String next) {
        return "{\"currentPassword\":\"" + current + "\",\"newPassword\":\"" + next + "\"}";
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials(email, password)));
    }

    private ResultActions refresh(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + token + "\"}"));
    }

    private ResultActions me(String access) throws Exception {
        return mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + access));
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static String accessTokenOf(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private static String refreshTokenOf(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.refreshToken");
    }

    /** Properly signed but expired 10 minutes ago (beyond the 30 s clock skew). */
    private String expiredToken(Long userId) {
        Instant past = Instant.now().minus(30, ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.security().jwt().issuer())
                .subject(String.valueOf(userId))
                .issuedAt(past)
                .expiresAt(past.plus(20, ChronoUnit.MINUTES))
                .claim("roles", List.of(Role.ADMIN.name()))
                .claim("sid", UUID.randomUUID().toString())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static boolean isTo(SimpleMailMessage message, String email) {
        return message != null && message.getTo() != null && Arrays.asList(message.getTo()).contains(email);
    }

    /** Waits (the reset mail is async) until exactly {@code count} mails reached the address; returns the last. */
    private SimpleMailMessage waitForMails(String email, int count) {
        verify(mailSender, timeout(5000).times(count)).send(argThat((SimpleMailMessage m) -> isTo(m, email)));
        return lastMailTo(email);
    }

    private SimpleMailMessage lastMailTo(String email) {
        verify(mailSender, timeout(5000).atLeastOnce()).send(argThat((SimpleMailMessage m) -> isTo(m, email)));
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, atLeastOnce()).send(captor.capture());
        List<SimpleMailMessage> mine = captor.getAllValues().stream().filter(m -> isTo(m, email)).toList();
        return mine.get(mine.size() - 1);
    }

    private static String tokenOf(SimpleMailMessage message) {
        Matcher matcher = Pattern.compile("token=([\\w-]+)").matcher(message.getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
