package com.example.app.config;

import com.example.app.common.security.JwtSecrets;
import com.example.app.common.security.PublicPostBearerTokenResolver;
import com.example.app.common.security.Role;
import com.example.app.common.security.SessionActiveValidator;
import com.example.app.common.security.SessionValidityChecker;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.DispatcherType;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Stateless API security: every request needs a Bearer JWT (HS256, claim {@code roles} -> {@code ROLE_*})
 * except the whitelist below (keep it in sync with docs/security.md). Authorization per controller is
 * done with the meta-annotations of {@code common.security} ({@code @AdminOnly}, ...).
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

    public static final String ROLES_CLAIM = "roles";

    static final String[] PUBLIC_POST_ENDPOINTS = {
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/api/v1/auth/invitations/accept",
        "/api/v1/auth/logout",
        "/api/v1/auth/password-reset/request",
        "/api/v1/auth/password-reset/confirm",
    };
    static final String[] PUBLIC_GET_ENDPOINTS = {"/actuator/health", "/actuator/health/**"};
    static final String[] DEV_ONLY_GET_ENDPOINTS = {"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"};

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, Environment environment,
            AuthenticationEntryPoint problemAuthenticationEntryPoint,
            AccessDeniedHandler problemAccessDeniedHandler,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            BearerTokenResolver bearerTokenResolver) throws Exception {
        boolean dev = environment.acceptsProfiles(Profiles.of("dev"));
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> { })
                .authorizeHttpRequests(auth -> {
                    // Container-internal error dispatches must not turn real errors into 401s.
                    auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    auth.requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll();
                    auth.requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll();
                    if (dev) {
                        auth.requestMatchers(HttpMethod.GET, DEV_ONLY_GET_ENDPOINTS).permitAll();
                    }
                    auth.requestMatchers("/api/v1/admin/**").hasRole(Role.ADMIN.name());
                    auth.requestMatchers("/api/v1/me/**").hasRole(Role.USER.name());
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(problemAuthenticationEntryPoint)
                        .accessDeniedHandler(problemAccessDeniedHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemAuthenticationEntryPoint)
                        .accessDeniedHandler(problemAccessDeniedHandler));
        return http.build();
    }

    /** Fails the startup with a clear message if JWT_SECRET is missing, not Base64 or shorter than 32 bytes. */
    @Bean
    SecretKey jwtSecretKey(AppProperties properties) {
        return JwtSecrets.hmacSha256Key(properties.security().jwt().secret());
    }

    /**
     * Signature, timestamps and issuer first; only a token that passes them is checked against the session
     * store (claim {@code sid}, ADR 0009), so expired or forged tokens never cost a lookup.
     */
    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey, AppProperties properties,
            SessionValidityChecker sessionValidityChecker) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        OAuth2TokenValidator<Jwt> standard = new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ofSeconds(30)),
                new JwtIssuerValidator(properties.security().jwt().issuer()));
        OAuth2TokenValidator<Jwt> session = new SessionActiveValidator(sessionValidityChecker);
        decoder.setJwtValidator(jwt -> {
            OAuth2TokenValidatorResult result = standard.validate(jwt);
            return result.hasErrors() ? result : session.validate(jwt);
        });
        return decoder;
    }

    /** The public POST endpoints ignore the Authorization header (an expired Bearer must not break logout). */
    @Bean
    BearerTokenResolver bearerTokenResolver() {
        return new PublicPostBearerTokenResolver(Set.of(PUBLIC_POST_ENDPOINTS));
    }

    /** Used by the auth module (Phase 2) to issue access tokens (HS256 with the same key). */
    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(jwtSecretKey));
    }

    @Bean
    org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(ROLES_CLAIM);
        authorities.setAuthorityPrefix(Role.AUTHORITY_PREFIX);
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /** 401 as ProblemDetail, rendered by {@code GlobalExceptionHandler} so the format is identical. */
    @Bean
    AuthenticationEntryPoint problemAuthenticationEntryPoint(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        return (request, response, ex) -> resolver.resolveException(request, response, null, ex);
    }

    /** 403 as ProblemDetail, rendered by {@code GlobalExceptionHandler} so the format is identical. */
    @Bean
    AccessDeniedHandler problemAccessDeniedHandler(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        return (request, response, ex) -> resolver.resolveException(request, response, null, ex);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.cors().allowedOrigins().stream().map(String::strip).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT,
                HttpHeaders.ACCEPT_LANGUAGE, "X-Requested-With"));
        cors.setExposedHeaders(List.of(HttpHeaders.LOCATION, HttpHeaders.WWW_AUTHENTICATE));
        cors.setAllowCredentials(false);
        cors.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
