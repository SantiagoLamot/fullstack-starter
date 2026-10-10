package com.example.app.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.app.common.exception.BusinessException;
import com.example.app.common.security.AdminOnly;
import com.example.app.common.security.CurrentUser;
import com.example.app.common.security.Role;
import com.example.app.modules.auth.service.RefreshTokenService;
import com.example.app.support.AbstractIntegrationTest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Import(SecurityIntegrationTest.ProbeController.class)
class SecurityIntegrationTest extends AbstractIntegrationTest {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String URN = "urn:app:problem:";

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private AppProperties properties;

    @Autowired
    private RefreshTokenService refreshTokenService;

    // ------------------------------------------------------------------ authentication

    @Test
    void protectedRouteWithoutTokenReturns401ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/anything"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("Bearer")))
                .andExpect(jsonPath("$.type").value(URN + "unauthorized"))
                .andExpect(jsonPath("$.title").value("No autenticado"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void invalidTokenReturns401ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/anything").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(URN + "unauthorized"));
    }

    @Test
    void tokenWithWrongIssuerIsRejected() throws Exception {
        String token = token("42", List.of(Role.ADMIN), "someone-else");
        mockMvc.perform(get("/api/v1/test-probe/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signedTokenWithoutSessionClaimIsRejected() throws Exception {
        String token = signed("42", List.of(Role.ADMIN), properties.security().jwt().issuer(), null);
        mockMvc.perform(get("/api/v1/test-probe/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(URN + "unauthorized"));
    }

    @Test
    void signedTokenWithUnknownSessionIsRejected() throws Exception {
        String token = signed("42", List.of(Role.ADMIN), properties.security().jwt().issuer(),
                java.util.UUID.randomUUID().toString());
        mockMvc.perform(get("/api/v1/test-probe/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicAuthEndpointsIgnoreAnInvalidBearer() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/auth/refresh").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("La sesión es inválida o expiró. Iniciá sesión de nuevo."));
    }

    @Test
    void authenticatedUnknownRouteReturns404ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/anything").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(URN + "not-found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }

    @Test
    void apiDocsAreNotPublicOutsideDevProfile() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ authorization (real signed JWT)

    @Test
    void realTokenWithAdminRoleIsAcceptedAndCurrentUserComesFromSub() throws Exception {
        String token = token("42", List.of(Role.ADMIN), properties.security().jwt().issuer());
        mockMvc.perform(get("/api/v1/test-probe/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(42));
    }

    @Test
    void realTokenWithoutRequiredRoleReturns403ProblemDetail() throws Exception {
        String token = token("7", List.of(Role.USER), properties.security().jwt().issuer());
        mockMvc.perform(get("/api/v1/test-probe/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(URN + "forbidden"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void audiencePrefixesAreEnforcedByTheFilterChain() throws Exception {
        String issuer = properties.security().jwt().issuer();
        String user = "Bearer " + token("7", List.of(Role.USER), issuer);
        
        String admin = "Bearer " + token("9", List.of(Role.ADMIN), issuer);

        mockMvc.perform(get("/api/v1/admin/x").header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/me/x").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isForbidden());
        // allowed roles pass the chain (404: the route does not exist)
        mockMvc.perform(get("/api/v1/admin/x").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/me/x").header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ error format

    @Test
    void invalidBodyReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/test-probe").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(URN + "validation-error"))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").value("debe ser un email válido"))
                .andExpect(jsonPath("$.errors[1].field").value("name"))
                .andExpect(jsonPath("$.errors[1].message").value("es obligatorio"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/test-probe").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(URN + "bad-request"))
                .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    void businessExceptionReturns422() throws Exception {
        mockMvc.perform(get("/api/v1/test-probe/business-error").with(admin()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value(URN + "business-rule"))
                .andExpect(jsonPath("$.detail").value("La inscripción está cerrada."))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void unexpectedExceptionReturns500WithoutInternalDetails() throws Exception {
        mockMvc.perform(get("/api/v1/test-probe/crash").with(admin()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value(URN + "internal-error"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("secret-internal-detail"))));
    }

    // ------------------------------------------------------------------ helpers

    private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject("1")).authorities(new SimpleGrantedAuthority(Role.ADMIN.authority()));
    }

    /** Signed token with a live session (claim sid = a real refresh family of that subject). */
    private String token(String subject, List<Role> roles, String issuer) {
        String sessionId = refreshTokenService.startFamily(Long.valueOf(subject)).familyId();
        return signed(subject, roles, issuer, sessionId);
    }

    private String signed(String subject, List<Role> roles, String issuer, String sessionId) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plus(5, ChronoUnit.MINUTES))
                .claim("roles", roles.stream().map(Role::name).toList());
        if (sessionId != null) {
            claims.claim("sid", sessionId);
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    record ProbeRequest(@NotBlank String name, @NotBlank @Email String email) {
    }

    @RestController
    @RequestMapping("/api/v1/test-probe")
    @Validated
    @AdminOnly
    static class ProbeController {

        @GetMapping("/me")
        Map<String, Object> me(@CurrentUser Long userId) {
            return Map.of("userId", userId);
        }

        @PostMapping
        ProbeRequest create(@Valid @RequestBody ProbeRequest request) {
            return request;
        }

        @GetMapping("/business-error")
        void businessError() {
            throw new BusinessException("La inscripción está cerrada.");
        }

        @GetMapping("/crash")
        void crash() {
            throw new IllegalStateException("secret-internal-detail");
        }
    }
}
