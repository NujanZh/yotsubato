package io.github.nujanzh.yotsubato.security;

import io.github.nujanzh.yotsubato.exception.JwtValidationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    // Maybe change /actuator/ to /actuator/health/
    private static final Set<String> PUBLIC_PREFIXES =
            Set.of("/auth/", "/api-docs/", "/swagger-ui/", "/actuator/");

    private final JwtService jwtService;
    private final HandlerExceptionResolver resolver;

    public JwtAuthFilter(
            JwtService jwtService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.jwtService = jwtService;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader(AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthenticatedPrincipal principal;

        try {
            final String token = authHeader.substring(TOKEN_PREFIX.length());
            principal = jwtService.parseAndValidate(token);
        } catch (JwtValidationException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
            resolver.resolveException(request, response, null, ex);
            return;
        }

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
