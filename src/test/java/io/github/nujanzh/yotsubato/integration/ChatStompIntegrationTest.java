package io.github.nujanzh.yotsubato.integration;

import io.github.nujanzh.yotsubato.dto.message.MessageResponse;
import io.github.nujanzh.yotsubato.dto.message.SendMessageRequest;
import io.github.nujanzh.yotsubato.dto.message.StompError;
import io.github.nujanzh.yotsubato.dto.room.CreateRoomRequest;
import io.github.nujanzh.yotsubato.dto.room.RoomDetail;
import io.github.nujanzh.yotsubato.model.message.MessageType;
import io.github.nujanzh.yotsubato.model.room.RoomType;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatStompIntegrationTest extends IntegrationTest {

    @Test
    void sendMessage_broadcastsToTopicSubscribers() throws Exception {
        TestUser sender = registerUser();
        UUID roomId = createPublicRoom(sender.accessToken());

        StompSession session = connect(sender.accessToken());
        BlockingQueue<MessageResponse> received =
                subscribe(session, "/topic/rooms/" + roomId, MessageResponse.class);

        MessageResponse msg =
                sendUntilReceived(
                        received,
                        () ->
                                session.send(
                                        "/app/rooms/" + roomId + "/message",
                                        new SendMessageRequest(
                                                "hello world", MessageType.TEXT, null)));

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
        BlockingQueue<MessageResponse> received =
                subscribe(session, "/topic/rooms/" + roomId, MessageResponse.class);

        StompHeaders headers = new StompHeaders();
        headers.setDestination("/app/rooms/" + roomId + "/message");
        headers.add("clientMessageId", "client-123");

        MessageResponse msg =
                sendUntilReceived(
                        received,
                        () ->
                                session.send(
                                        headers,
                                        new SendMessageRequest("hi", MessageType.TEXT, null)));

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
