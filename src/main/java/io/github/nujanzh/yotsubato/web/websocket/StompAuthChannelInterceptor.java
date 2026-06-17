package io.github.nujanzh.yotsubato.web.websocket;

import io.github.nujanzh.yotsubato.exception.RoomNotFoundException;
import io.github.nujanzh.yotsubato.repository.room.RoomMemberRepository;
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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final RoomMemberRepository roomMemberRepository;
    private final Clock clock;
    private static final AntPathMatcher matcher = new AntPathMatcher();

    public StompAuthChannelInterceptor(
            JwtService jwtService, RoomMemberRepository roomMemberRepository, Clock clock) {
        this.jwtService = jwtService;
        this.roomMemberRepository = roomMemberRepository;
        this.clock = clock;
    }

    @Override
    public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            log.error("Accessor not found in message");
            throw new IllegalStateException("StompHeaderAccessor missing from STOMP message");
        }

        StompCommand command = accessor.getCommand();

        switch (command) {
            case CONNECT -> authenticateConnect(accessor);
            case SEND -> requireActivePrincipal(accessor);
            case SUBSCRIBE ->
                    authorizeSubscription(
                            accessor.getDestination(), requireActivePrincipal(accessor));
            case null -> {
                // if command null just return a message
            }
            default -> {
                // ignore other commands
            }
        }

        return message;
    }

    private AuthenticatedPrincipal requireAuthenticatedPrincipal(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof Authentication authentication)) {
            log.warn("Authentication not found in message");
            throw new BadCredentialsException("Authentication failed");
        }

        if (!(authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
            log.warn("Principal not found in message");
            throw new BadCredentialsException("Authentication failed");
        }

        return principal;
    }

    private AuthenticatedPrincipal requireActivePrincipal(StompHeaderAccessor accessor) {
        AuthenticatedPrincipal principal = requireAuthenticatedPrincipal(accessor);

        Instant now = Instant.now(clock);

        if (!now.isBefore(principal.expiresAt())) {
            log.debug("Session expired");
            throw new BadCredentialsException("Session expired");
        }

        return principal;
    }

    private void authorizeSubscription(String destination, AuthenticatedPrincipal principal) {
        if (destination == null) {
            // TODO: add custom exception

            log.error("Destination not found in message");
            throw new BadCredentialsException("Destination not found in STOMP message");
        }

        if (matcher.match(WebSocketDestination.ROOM_TOPIC_PATTERN, destination)) {
            String roomIdString =
                    matcher.extractUriTemplateVariables(
                                    WebSocketDestination.ROOM_TOPIC_PATTERN, destination)
                            .get("roomId");

            UUID roomId;

            try {
                roomId = UUID.fromString(roomIdString);
            } catch (IllegalArgumentException ex) {
                throw new RoomNotFoundException("Invalid room ID format");
            }

            boolean isMember =
                    roomMemberRepository.existsByRoomIdAndUserId(roomId, principal.userId());

            if (!isMember) {
                throw new RoomNotFoundException("Room not found: " + roomId);
            }
        }
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
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
}
