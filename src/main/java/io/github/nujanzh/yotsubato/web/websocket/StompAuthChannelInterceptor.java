package io.github.nujanzh.yotsubato.web.websocket;

import io.github.nujanzh.yotsubato.exception.RoomNotFoundException;
import io.github.nujanzh.yotsubato.repository.room.RoomMemberRepository;
import io.github.nujanzh.yotsubato.repository.room.RoomRepository;
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

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final RoomMemberRepository roomMemberRepository;
    private static final AntPathMatcher matcher = new AntPathMatcher();
    private static final String SUBSCRIBE_DESTINATION_PATTERN = "/topic/rooms/{roomId}";

    public StompAuthChannelInterceptor(
            JwtService jwtService, RoomMemberRepository roomMemberRepository) {
        this.jwtService = jwtService;
        this.roomMemberRepository = roomMemberRepository;
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

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            if (accessor.getUser() == null) {
                log.warn("User not authenticated");
                throw new BadCredentialsException("Authentication failed");
            }

            String destination = accessor.getDestination();

            if (destination == null) {
                // TODO: add custom exception

                log.error("Destination not found in message");
                throw new BadCredentialsException("Destination not found in STOMP message");
            }

            if (matcher.match(SUBSCRIBE_DESTINATION_PATTERN, destination)) {
                String roomIdString =
                        matcher.extractUriTemplateVariables(
                                        SUBSCRIBE_DESTINATION_PATTERN, destination)
                                .get("roomId");

                UUID roomId;

                try {
                    roomId = UUID.fromString(roomIdString);
                } catch (IllegalArgumentException ex) {
                    throw new RoomNotFoundException("Invalid room ID format");
                }

                AuthenticatedPrincipal user =
                        (AuthenticatedPrincipal)
                                ((Authentication) accessor.getUser()).getPrincipal();

                boolean isMember =
                        roomMemberRepository.existsByRoomIdAndUserId(roomId, user.userId());

                if (!isMember) {
                    throw new RoomNotFoundException("Room not found: " + roomId);
                }
            }
        }

        return message;
    }
}
