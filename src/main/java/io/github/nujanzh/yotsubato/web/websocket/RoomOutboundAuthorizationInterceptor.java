package io.github.nujanzh.yotsubato.web.websocket;

import io.github.nujanzh.yotsubato.repository.room.RoomMemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class RoomOutboundAuthorizationInterceptor implements ChannelInterceptor {

    private final RoomMemberRepository roomMemberRepository;
    private final WebSocketSessionRegistry sessionRegistry;
    private static final AntPathMatcher matcher = new AntPathMatcher();

    public RoomOutboundAuthorizationInterceptor(
            RoomMemberRepository roomMemberRepository, WebSocketSessionRegistry sessionRegistry) {
        this.roomMemberRepository = roomMemberRepository;
        this.sessionRegistry = sessionRegistry;
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

        Optional<UUID> userId = sessionRegistry.findUserId(sessionId);

        if (userId.isEmpty()) {
            log.debug(
                    "User ID not found for session. session={}, destination={}",
                    sessionId,
                    destination);
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

        boolean isMember = roomMemberRepository.existsByRoomIdAndUserId(roomId, userId.get());

        if (!isMember) {
            log.debug("User not a member. user={}, room={}", userId.get(), roomId);
            return null;
        }

        return message;
    }
}
