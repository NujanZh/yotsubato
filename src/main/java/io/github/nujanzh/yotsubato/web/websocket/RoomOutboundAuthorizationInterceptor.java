package io.github.nujanzh.yotsubato.web.websocket;

import io.github.nujanzh.yotsubato.repository.room.RoomMemberRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Slf4j
@Component
public class RoomOutboundAuthorizationInterceptor implements ChannelInterceptor {

    private final RoomMemberRepository roomMemberRepository;
    private final WebSocketSessionRegistry sessionRegistry;
    private final Clock clock;
    private static final AntPathMatcher matcher = new AntPathMatcher();

    public RoomOutboundAuthorizationInterceptor(
            RoomMemberRepository roomMemberRepository,
            WebSocketSessionRegistry sessionRegistry,
            Clock clock) {
        this.roomMemberRepository = roomMemberRepository;
        this.sessionRegistry = sessionRegistry;
        this.clock = clock;
    }

    @Override
    public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);

        if (accessor.getMessageType() != SimpMessageType.MESSAGE) {
            return message;
        }

        String destination = accessor.getDestination();

        if (destination == null
                || !matcher.match(WebSocketDestination.ROOM_TOPIC_PATTERN, destination)) {
            return message;
        }

        String sessionId = accessor.getSessionId();

        if (sessionId == null) {
            log.warn("Missing session ID, destination={}", destination);
            return null;
        }

        Optional<WebSocketSessionInfo> sessionInfoOptional =
                sessionRegistry.findBySessionId(sessionId);

        if (sessionInfoOptional.isEmpty()) {
            log.debug(
                    "Session Info not found for session. session={}, destination={}",
                    sessionId,
                    destination);
            return null;
        }

        WebSocketSessionInfo sessionInfo = sessionInfoOptional.get();

        // TODO: Maybe move into helper so both Interceptor can use it
        Instant now = Instant.now(clock);

        if (!now.isBefore(sessionInfo.expiresAt())) {
            log.debug("Session expired. session={}, destination={}", sessionId, destination);
            return null;
        }

        String roomIdString =
                matcher.extractUriTemplateVariables(
                                WebSocketDestination.ROOM_TOPIC_PATTERN, destination)
                        .get("roomId");

        UUID roomId;

        try {
            roomId = UUID.fromString(roomIdString);
        } catch (IllegalArgumentException | NullPointerException ex) {
            log.warn("Invalid room ID: {}", roomIdString);
            return null;
        }

        boolean isMember =
                roomMemberRepository.existsByRoomIdAndUserId(roomId, sessionInfo.userId());

        if (!isMember) {
            log.debug("User not a member. user={}, room={}", sessionInfo.userId(), roomId);
            return null;
        }

        return message;
    }
}
