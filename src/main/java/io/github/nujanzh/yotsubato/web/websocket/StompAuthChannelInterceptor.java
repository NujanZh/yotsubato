package io.github.nujanzh.yotsubato.web.websocket;

import io.github.nujanzh.yotsubato.security.jwt.JwtValidationException;
import io.github.nujanzh.yotsubato.security.jwt.BearerAuthConstants;
import io.github.nujanzh.yotsubato.security.jwt.JwtService;
import io.github.nujanzh.yotsubato.security.userdetails.AuthenticatedPrincipal;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public StompAuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            log.error("Accessor not found in message");
            throw new IllegalStateException("StompHeaderAccessor missing from STOMP message");
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader(BearerAuthConstants.AUTHORIZATION_HEADER);

            if (header == null || !header.startsWith(BearerAuthConstants.BEARER_PREFIX)) {
                log.warn("Missing or invalid authorization header");
                throw new BadCredentialsException("Authentication failed");
            }

            AuthenticatedPrincipal principal;

            try {
                String token = header.substring(BearerAuthConstants.BEARER_PREFIX.length());
                principal = jwtService.parseAndValidate(token);
            } catch (JwtValidationException ex) {
                log.debug("JWT validation failed: {}", ex.getMessage());
                throw new BadCredentialsException("Authentication failed", ex);
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, List.of());

            accessor.setUser(auth);
        }

        return message;
    }
}
