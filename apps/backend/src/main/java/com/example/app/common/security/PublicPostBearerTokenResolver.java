package com.example.app.common.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

/**
 * Ignores the {@code Authorization} header on the public POST endpoints (login, refresh, logout, ...), so an
 * expired or invalid Bearer cannot turn them into a 401 (ADR 0009). Everywhere else delegates to the default.
 */
public class PublicPostBearerTokenResolver implements BearerTokenResolver {

    private final Set<String> publicPostPaths;
    private final BearerTokenResolver delegate = new DefaultBearerTokenResolver();

    public PublicPostBearerTokenResolver(Set<String> publicPostPaths) {
        this.publicPostPaths = Set.copyOf(publicPostPaths);
    }

    @Override
    public String resolve(HttpServletRequest request) {
        if ("POST".equalsIgnoreCase(request.getMethod()) && publicPostPaths.contains(pathOf(request))) {
            return null;
        }
        return delegate.resolve(request);
    }

    private static String pathOf(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        String path = context != null && !context.isEmpty() && uri.startsWith(context)
                ? uri.substring(context.length()) : uri;
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }
}
