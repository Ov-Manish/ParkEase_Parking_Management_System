package com.parkease.api_gateway.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class GatewaySecurityFilter extends OncePerRequestFilter {

    private final JwtValidator jwtValidator;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Whitelisted public endpoints
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/**",
            "/actuator/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    );

    public GatewaySecurityFilter(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Allow preflight OPTIONS requests to pass to CorsFilter
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestPath = request.getRequestURI();

        // 1. Check if the path is publicly accessible (or public GET on lots)
        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestPath))
                || ("GET".equalsIgnoreCase(request.getMethod()) && pathMatcher.match("/api/v1/lots/**", requestPath));

        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        HeaderMapRequestWrapper requestWrapper = new HeaderMapRequestWrapper(request);
        requestWrapper.addHeader("X-Correlation-Id", correlationId);
        response.setHeader("X-Correlation-Id", correlationId);

        if (isPublic) {
            filterChain.doFilter(requestWrapper, response);
            return;
        }

        // 2. Validate Bearer Token on Protected Paths
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header", requestPath);
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtValidator.validateToken(token)) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token", requestPath);
            return;
        }

        // 3. Extract claims and propagate downstream
        Claims claims = jwtValidator.getClaims(token);
        String userId = claims.getSubject();
        String role = claims.get("role", String.class);
        String email = claims.get("email", String.class);

        if (userId != null) requestWrapper.addHeader("X-User-Id", userId);
        if (role != null) requestWrapper.addHeader("X-User-Role", role);
        if (email != null) requestWrapper.addHeader("X-User-Email", email);

        filterChain.doFilter(requestWrapper, response);
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String json = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now(), status.value(), status.getReasonPhrase(), message, path
        );
        response.getWriter().write(json);
    }
}