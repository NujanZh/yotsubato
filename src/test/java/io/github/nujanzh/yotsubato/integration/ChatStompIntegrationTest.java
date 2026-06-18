package io.github.nujanzh.yotsubato.integration;

import static org.assertj.core.api.Assertions.*;

import io.github.nujanzh.yotsubato.MutableClock;
import io.github.nujanzh.yotsubato.MutableClockTestConfig;
import io.github.nujanzh.yotsubato.dto.member.AddMemberRequest;
import io.github.nujanzh.yotsubato.dto.message.*;
import io.github.nujanzh.yotsubato.dto.room.CreateRoomRequest;
import io.github.nujanzh.yotsubato.dto.room.RoomDetail;
import io.github.nujanzh.yotsubato.model.message.MessageType;
import io.github.nujanzh.yotsubato.model.room.RoomType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@Import(MutableClockTestConfig.class)
class ChatStompIntegrationTest extends IntegrationTest {

    @Autowired private MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.setInstant(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void sendMessage_broadcastsToTopicSubscribers() throws Exception {
        TestUser sender = registerUser();
        UUID roomId = createPublicRoom(sender.accessToken());

        StompSession session = connect(sender.accessToken());
        BlockingQueue<RoomEvent> received =
                subscribe(session, "/topic/rooms/" + roomId, RoomEvent.class);

        RoomEvent event =
                sendUntilReceived(
                        received,
                        () ->
                                session.send(
                                        "/app/rooms/" + roomId + "/message",
                                        new SendMessageRequest(
                                                "hello world", MessageType.TEXT, null)));

        assertThat(event).isExactlyInstanceOf(RoomEvent.MessageCreatedEvent.class);

        RoomEvent.MessageCreatedEvent createdEvent = (RoomEvent.MessageCreatedEvent) event;
        MessageResponse msg = createdEvent.message();

        assertThat(msg).isNotNull();
        assertThat(msg.content()).isEqualTo("hello world");
        assertThat(msg.roomId()).isEqualTo(roomId);
        assertThat(msg.sender().id()).isEqualTo(sender.id());

        session.disconnect();
    }

    @Test
    void sendMessage_preservesClientMessageId() throws Exception {
        TestUser sender = registerUser();
        UUID roomId = createPublicRoom(sender.accessToken());

        StompSession session = connect(sender.accessToken());
        BlockingQueue<RoomEvent> received =
                subscribe(session, "/topic/rooms/" + roomId, RoomEvent.class);

        StompHeaders headers = new StompHeaders();
        headers.setDestination("/app/rooms/" + roomId + "/message");
        headers.add("clientMessageId", "client-123");

        RoomEvent event =
                sendUntilReceived(
                        received,
                        () ->
                                session.send(
                                        headers,
                                        new SendMessageRequest("hi", MessageType.TEXT, null)));

        assertThat(event).isExactlyInstanceOf(RoomEvent.MessageCreatedEvent.class);

        RoomEvent.MessageCreatedEvent createdEvent = (RoomEvent.MessageCreatedEvent) event;
        MessageResponse msg = createdEvent.message();

        assertThat(msg).isNotNull();
        assertThat(msg.clientMessageId()).isEqualTo("client-123");

        session.disconnect();
    }

    @Test
    void sendMessage_asNonMember_receivesStompError() throws Exception {
        TestUser owner = registerUser();
        TestUser outsider = registerUser();
        UUID roomId = createPublicRoom(owner.accessToken());

        StompSession session = connect(outsider.accessToken());
        BlockingQueue<StompError> errors =
                subscribe(session, "/user/queue/error", StompError.class);

        StompError error =
                sendUntilReceived(
                        errors,
                        () ->
                                session.send(
                                        "/app/rooms/" + roomId + "/message",
                                        new SendMessageRequest("hi", MessageType.TEXT, null)));

        assertThat(error).isNotNull();
        assertThat(error.code()).isEqualTo(404);

        session.disconnect();
    }

    @Test
    void editMessage_viaRest_broadcastsMessageEditedEvent() throws Exception {
        TestUser sender = registerUser();
        UUID roomId = createPublicRoom(sender.accessToken());

        StompSession session = connect(sender.accessToken());
        BlockingQueue<RoomEvent> received =
                subscribe(session, "/topic/rooms/" + roomId, RoomEvent.class);

        RoomEvent createdEvent =
                sendUntilReceived(
                        received,
                        () ->
                                session.send(
                                        "/app/rooms/" + roomId + "/message",
                                        new SendMessageRequest(
                                                "before edit", MessageType.TEXT, null)));

        assertThat(createdEvent).isExactlyInstanceOf(RoomEvent.MessageCreatedEvent.class);
        UUID msgId = ((RoomEvent.MessageCreatedEvent) createdEvent).message().id();

        received.clear();

        authedClient(sender.accessToken())
                .patch()
                .uri("/rooms/{roomId}/messages/{messageId}", roomId, msgId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateMessageRequest("after edit"))
                .exchange()
                .expectStatus()
                .isOk();

        RoomEvent event = received.poll(500, TimeUnit.MILLISECONDS);

        assertThat(event).isNotNull().isExactlyInstanceOf(RoomEvent.MessageEditedEvent.class);

        RoomEvent.MessageEditedEvent editedEvent = (RoomEvent.MessageEditedEvent) event;

        assertThat(editedEvent.message().id()).isEqualTo(msgId);
        assertThat(editedEvent.message().roomId()).isEqualTo(roomId);
        assertThat(editedEvent.message().content()).isEqualTo("after edit");
        assertThat(editedEvent.message().editedAt()).isNotNull();

        session.disconnect();
    }

    @Test
    void typing_asRoomMember_broadcastsTypingEvent() throws Exception {
        TestUser sender = registerUser();
        UUID roomId = createPublicRoom(sender.accessToken());

        StompSession session = connect(sender.accessToken());
        BlockingQueue<RoomEvent> received =
                subscribe(session, "/topic/rooms/" + roomId, RoomEvent.class);

        RoomEvent typingEvent =
                sendUntilReceived(
                        received,
                        () ->
                                session.send(
                                        "/app/rooms/" + roomId + "/typing",
                                        new TypingRequest(true)));

        assertThat(typingEvent).isNotNull().isExactlyInstanceOf(RoomEvent.UserTypingEvent.class);
        assertThat(((RoomEvent.UserTypingEvent) typingEvent).userId()).isEqualTo(sender.id());
        assertThat(((RoomEvent.UserTypingEvent) typingEvent).typing()).isTrue();
    }

    @Test
    void deleteMessage_viaRest_broadcastsMessageDeletedEvent() throws Exception {
        TestUser sender = registerUser();
        UUID roomId = createPublicRoom(sender.accessToken());

        StompSession session = connect(sender.accessToken());
        BlockingQueue<RoomEvent> received =
                subscribe(session, "/topic/rooms/" + roomId, RoomEvent.class);

        RoomEvent createdEvent =
                sendUntilReceived(
                        received,
                        () ->
                                session.send(
                                        "/app/rooms/" + roomId + "/message",
                                        new SendMessageRequest(
                                                "hello world", MessageType.TEXT, null)));

        assertThat(createdEvent).isExactlyInstanceOf(RoomEvent.MessageCreatedEvent.class);
        UUID msgId = ((RoomEvent.MessageCreatedEvent) createdEvent).message().id();

        authedClient(sender.accessToken())
                .post()
                .uri("/rooms/{id}/messages/delete", roomId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new DeleteMessageRequest(List.of(msgId)))
                .exchange()
                .expectStatus()
                .isNoContent();

        RoomEvent event = received.poll(500, TimeUnit.MILLISECONDS);

        assertThat(event).isNotNull().isExactlyInstanceOf(RoomEvent.MessageDeletedEvent.class);

        RoomEvent.MessageDeletedEvent deletedEvent = (RoomEvent.MessageDeletedEvent) event;

        assertThat(deletedEvent.messageIds()).isNotEmpty().contains(msgId);

        session.disconnect();
    }

    @Test
    void removedMember_doesNotReceiveSubsequentRoomEvents() throws Exception {
        TestUser admin = registerUser();
        TestUser member = registerUser();
        RoomDetail room =
                authedClient(admin.accessToken())
                        .post()
                        .uri("/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                new CreateRoomRequest(
                                        "Room " + uniqueSuffix(), RoomType.PRIVATE, "a room", null))
                        .exchange()
                        .expectStatus()
                        .isCreated()
                        .expectBody(RoomDetail.class)
                        .returnResult()
                        .getResponseBody();

        StompSession adminSession = connect(admin.accessToken());
        BlockingQueue<RoomEvent> adminQueue =
                subscribe(adminSession, "/topic/rooms/" + room.id(), RoomEvent.class);

        authedClient(admin.accessToken())
                .post()
                .uri("/rooms/{id}/members", room.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AddMemberRequest(member.id()))
                .exchange()
                .expectStatus()
                .isCreated();

        StompSession memberSession = connect(member.accessToken());

        BlockingQueue<RoomEvent> memberQueue =
                subscribe(memberSession, "/topic/rooms/" + room.id(), RoomEvent.class);

        RoomEvent adminEvent =
                sendUntilReceived(
                        adminQueue,
                        () ->
                                adminSession.send(
                                        "/app/rooms/" + room.id() + "/message",
                                        new SendMessageRequest(
                                                "Before removal", MessageType.TEXT, null)));

        assertThat(adminEvent).isNotNull().isExactlyInstanceOf(RoomEvent.MessageCreatedEvent.class);

        RoomEvent memberEvent = memberQueue.poll(500, TimeUnit.MILLISECONDS);
        assertThat(memberEvent)
                .isNotNull()
                .isExactlyInstanceOf(RoomEvent.MessageCreatedEvent.class);

        authedClient(admin.accessToken())
                .delete()
                .uri("/rooms/{id}/members/{userId}", room.id(), member.id())
                .exchange()
                .expectStatus()
                .isOk();

        adminQueue.clear();
        memberQueue.clear();

        adminEvent =
                sendUntilReceived(
                        adminQueue,
                        () ->
                                adminSession.send(
                                        "/app/rooms/" + room.id() + "/message",
                                        new SendMessageRequest(
                                                "After removal", MessageType.TEXT, null)));

        assertThat(adminEvent).isNotNull().isExactlyInstanceOf(RoomEvent.MessageCreatedEvent.class);

        memberEvent = memberQueue.poll(500, TimeUnit.MILLISECONDS);
        assertThat(memberEvent).isNull();

        adminSession.disconnect();
        memberSession.disconnect();
    }

    @Test
    void sendMessage_afterTokenExpiration_isRejected() throws Exception {
        TestUser sender = registerUser();
        UUID roomId = createPublicRoom(sender.accessToken());

        StompSession session = connect(sender.accessToken());

        BlockingQueue<RoomEvent> roomEvents =
                subscribe(session, "/topic/rooms/" + roomId, RoomEvent.class);

        clock.advance(Duration.ofMinutes(16));

        try {
            session.send(
                    "/app/rooms/" + roomId + "/message",
                    new SendMessageRequest("after expiry", MessageType.TEXT, null));
        } catch (IllegalStateException ignored) {
            // connection already closed, also acceptable
        }

        RoomEvent event = roomEvents.poll(500, TimeUnit.MILLISECONDS);
        assertThat(event).isNull();

        if (session.isConnected()) {
            session.disconnect();
        }
    }

    @Test
    void connect_withoutAuthHeader_isRejected() {
        CompletableFuture<StompSession> future = connectAsync(new StompHeaders());
        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS));
    }

    @Test
    void connect_withInvalidToken_isRejected() {
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer not-a-real-token");
        CompletableFuture<StompSession> future = connectAsync(headers);
        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS));
    }

    private String wsUrl() {
        return "ws://localhost:" + port + "/api/ws";
    }

    private WebSocketStompClient stompClient() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());
        return stompClient;
    }

    private StompSession connect(String token) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);
        return connectAsync(connectHeaders).get(5, TimeUnit.SECONDS);
    }

    private CompletableFuture<StompSession> connectAsync(StompHeaders connectHeaders) {
        return stompClient()
                .connectAsync(
                        wsUrl(),
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {});
    }

    private <T> BlockingQueue<T> subscribe(
            StompSession session, String destination, Class<T> type) {
        BlockingQueue<T> queue = new LinkedBlockingQueue<>();
        session.subscribe(destination, collectingHandler(type, queue));
        return queue;
    }

    private <T> T sendUntilReceived(BlockingQueue<T> queue, Runnable send)
            throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            send.run();
            T item = queue.poll(500, TimeUnit.MILLISECONDS);
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    private <T> StompFrameHandler collectingHandler(Class<T> type, BlockingQueue<T> queue) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return type;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((T) payload);
            }
        };
    }

    private UUID createPublicRoom(String token) {
        return authedClient(token)
                .post()
                .uri("/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        new CreateRoomRequest(
                                "Room " + uniqueSuffix(), RoomType.PUBLIC, "a room", null))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(RoomDetail.class)
                .returnResult()
                .getResponseBody()
                .id();
    }
}
