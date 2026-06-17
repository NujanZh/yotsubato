package io.github.nujanzh.yotsubato.security.jwt;

import io.github.nujanzh.yotsubato.security.userdetails.AuthenticatedPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final RequestMatcher publicEndPoints;

    private final JwtService jwtService;
    private final HandlerExceptionResolver resolver;

    public JwtAuthFilter(
            JwtService jwtService,
            RequestMatcher publicEndPoints,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.jwtService = jwtService;
        this.publicEndPoints = publicEndPoints;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(BearerAuthConstants.AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BearerAuthConstants.BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthenticatedPrincipal principal;

        try {
            String token = authHeader.substring(BearerAuthConstants.BEARER_PREFIX.length());
            principal = jwtService.parseAndValidate(token);
        } catch (JwtValidationException ex) {
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
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return publicEndPoints.matches(request);
    }
}
