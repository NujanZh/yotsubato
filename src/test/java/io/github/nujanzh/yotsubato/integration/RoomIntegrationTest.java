package io.github.nujanzh.yotsubato.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

import io.github.nujanzh.yotsubato.dto.member.AddMemberRequest;
import io.github.nujanzh.yotsubato.dto.member.ChangeRoleRequest;
import io.github.nujanzh.yotsubato.dto.member.MemberInfo;
import io.github.nujanzh.yotsubato.dto.message.MessageResponse;
import io.github.nujanzh.yotsubato.dto.message.SendMessageRequest;
import io.github.nujanzh.yotsubato.dto.message.UpdateMessageRequest;
import io.github.nujanzh.yotsubato.dto.room.CreateRoomRequest;
import io.github.nujanzh.yotsubato.dto.room.RoomDetail;
import io.github.nujanzh.yotsubato.dto.room.RoomSummary;
import io.github.nujanzh.yotsubato.model.message.MessageType;
import io.github.nujanzh.yotsubato.model.room.MemberRole;
import io.github.nujanzh.yotsubato.model.room.RoomType;
import io.github.nujanzh.yotsubato.service.MessageService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class RoomIntegrationTest extends IntegrationTest {

    @Autowired private MessageService messageService;

    @Test
    void createRoom_asCreator_returns201WithCreatorAsAdmin() {
        TestUser creator = registerUser();

        RoomDetail room =
                authedClient(creator.accessToken())
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
                        .getResponseBody();

        assertThat(room).isNotNull();
        assertThat(room.members())
                .extracting(MemberInfo::userId, MemberInfo::role)
                .containsExactly(tuple(creator.id(), MemberRole.ADMIN));
    }

    @Test
    void createRoom_missingType_returns400() {
        TestUser creator = registerUser();

        authedClient(creator.accessToken())
                .post()
                .uri("/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateRoomRequest("Room " + uniqueSuffix(), null, "a room", null))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.errors[?(@.field=='type')]")
                .exists();
    }

    @Test
    void getAllRooms_returnsOnlyCallersRooms() {
        TestUser creator = registerUser();
        TestUser outsider = registerUser();
        RoomDetail room = createRoom(creator.accessToken(), RoomType.PUBLIC);

        RoomSummary[] creatorRooms =
                authedClient(creator.accessToken())
                        .get()
                        .uri("/rooms")
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(RoomSummary[].class)
                        .returnResult()
                        .getResponseBody();

        RoomSummary[] outsiderRooms =
                authedClient(outsider.accessToken())
                        .get()
                        .uri("/rooms")
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(RoomSummary[].class)
                        .returnResult()
                        .getResponseBody();

        assertThat(creatorRooms).extracting(RoomSummary::id).contains(room.id());
        assertThat(outsiderRooms).extracting(RoomSummary::id).doesNotContain(room.id());
    }

    @Test
    void getRoom_publicAsNonMember_returnsPreview() {
        TestUser creator = registerUser();
        TestUser outsider = registerUser();
        RoomDetail room = createRoom(creator.accessToken(), RoomType.PUBLIC);

        authedClient(outsider.accessToken())
                .get()
                .uri("/rooms/{id}", room.id())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.memberCount")
                .exists();
    }

    @Test
    void addMember_asAdmin_returns201() {
        TestUser admin = registerUser();
        TestUser invitee = registerUser();
        RoomDetail room = createRoom(admin.accessToken(), RoomType.PRIVATE);

        MemberInfo member =
                authedClient(admin.accessToken())
                        .post()
                        .uri("/rooms/{id}/members", room.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new AddMemberRequest(invitee.id()))
                        .exchange()
                        .expectStatus()
                        .isCreated()
                        .expectBody(MemberInfo.class)
                        .returnResult()
                        .getResponseBody();

        assertThat(member.userId()).isEqualTo(invitee.id());
        assertThat(member.role()).isEqualTo(MemberRole.MEMBER);
    }

    @Test
    void addMember_asNonAdmin_returns403() {
        TestUser admin = registerUser();
        TestUser member = registerUser();
        TestUser invitee = registerUser();
        RoomDetail room = createRoom(admin.accessToken(), RoomType.PUBLIC);

        authedClient(member.accessToken())
                .post()
                .uri("/rooms/{id}/join", room.id())
                .exchange()
                .expectStatus()
                .isCreated();

        authedClient(member.accessToken())
                .post()
                .uri("/rooms/{id}/members", room.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AddMemberRequest(invitee.id()))
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void addMember_unknownUserId_returns404() {
        TestUser admin = registerUser();
        RoomDetail room = createRoom(admin.accessToken(), RoomType.PRIVATE);

        authedClient(admin.accessToken())
                .post()
                .uri("/rooms/{id}/members", room.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AddMemberRequest(UUID.randomUUID()))
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void selfJoin_publicRoom_returns201() {
        TestUser creator = registerUser();
        TestUser joiner = registerUser();
        RoomDetail room = createRoom(creator.accessToken(), RoomType.PUBLIC);

        authedClient(joiner.accessToken())
                .post()
                .uri("/rooms/{id}/join", room.id())
                .exchange()
                .expectStatus()
                .isCreated();
    }

    @Test
    void selfJoin_privateRoom_returns400() {
        TestUser creator = registerUser();
        TestUser joiner = registerUser();
        RoomDetail room = createRoom(creator.accessToken(), RoomType.PRIVATE);

        authedClient(joiner.accessToken())
                .post()
                .uri("/rooms/{id}/join", room.id())
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void changeRole_asAdmin_returns200() {
        TestUser admin = registerUser();
        TestUser member = registerUser();
        RoomDetail room = createRoom(admin.accessToken(), RoomType.PRIVATE);

        authedClient(admin.accessToken())
                .post()
                .uri("/rooms/{id}/members", room.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AddMemberRequest(member.id()))
                .exchange()
                .expectStatus()
                .isCreated();

        MemberInfo updated =
                authedClient(admin.accessToken())
                        .patch()
                        .uri("/rooms/{id}/members/{userId}", room.id(), member.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ChangeRoleRequest(MemberRole.ADMIN))
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(MemberInfo.class)
                        .returnResult()
                        .getResponseBody();

        assertThat(updated.role()).isEqualTo(MemberRole.ADMIN);
    }

    @Test
    void editMessage_asSender_returns200WithUpdatedContent() {
        TestUser sender = registerUser();
        RoomDetail room = createRoom(sender.accessToken(), RoomType.PUBLIC);

        MessageResponse message =
                messageService.createMessage(
                        room.id(),
                        sender.id(),
                        new SendMessageRequest("before edit", MessageType.TEXT, null));

        MessageResponse updated =
                authedClient(sender.accessToken())
                        .patch()
                        .uri("/rooms/{roomId}/messages/{messageId}", room.id(), message.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new UpdateMessageRequest("after edit"))
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(MessageResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertThat(updated.content()).isEqualTo("after edit");
        assertThat(updated.editedAt()).isNotNull();
    }

    @Test
    void editMessage_asNonSender_returns403() {
        TestUser sender = registerUser();
        TestUser nonSender = registerUser();
        RoomDetail room = createRoom(sender.accessToken(), RoomType.PUBLIC);

        authedClient(sender.accessToken())
                .post()
                .uri("/rooms/{id}/members", room.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AddMemberRequest(nonSender.id()))
                .exchange()
                .expectStatus()
                .isCreated();

        MessageResponse message =
                messageService.createMessage(
                        room.id(),
                        sender.id(),
                        new SendMessageRequest("before edit", MessageType.TEXT, null));

        authedClient(nonSender.accessToken())
                .patch()
                .uri("/rooms/{roomId}/messages/{messageId}", room.id(), message.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateMessageRequest("after edit"))
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void editMessage_asNonMember_returns404() {
        TestUser sender = registerUser();
        TestUser nonMember = registerUser();
        RoomDetail room = createRoom(sender.accessToken(), RoomType.PUBLIC);

        MessageResponse message =
                messageService.createMessage(
                        room.id(),
                        sender.id(),
                        new SendMessageRequest("before edit", MessageType.TEXT, null));

        authedClient(nonMember.accessToken())
                .patch()
                .uri("/rooms/{roomId}/messages/{messageId}", room.id(), message.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateMessageRequest("after edit"))
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void editMessage_unknownMessageId_returns404() {
        TestUser sender = registerUser();
        RoomDetail room = createRoom(sender.accessToken(), RoomType.PUBLIC);

        authedClient(sender.accessToken())
                .patch()
                .uri("/rooms/{roomId}/messages/{messageId}", room.id(), UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateMessageRequest("after edit"))
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void editMessage_withBlankContent_returns400() {
        TestUser sender = registerUser();
        RoomDetail room = createRoom(sender.accessToken(), RoomType.PUBLIC);

        MessageResponse message =
                messageService.createMessage(
                        room.id(),
                        sender.id(),
                        new SendMessageRequest("before edit", MessageType.TEXT, null));

        authedClient(sender.accessToken())
                .patch()
                .uri("/rooms/{roomId}/messages/{messageId}", room.id(), message.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateMessageRequest(" "))
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void leaveRoom_asMember_returns200() {
        TestUser creator = registerUser();
        TestUser member = registerUser();
        RoomDetail room = createRoom(creator.accessToken(), RoomType.PUBLIC);

        authedClient(member.accessToken())
                .post()
                .uri("/rooms/{id}/join", room.id())
                .exchange()
                .expectStatus()
                .isCreated();

        authedClient(member.accessToken())
                .delete()
                .uri("/rooms/{id}/members/me", room.id())
                .exchange()
                .expectStatus()
                .isOk();
    }

    private RoomDetail createRoom(String token, RoomType type) {
        return authedClient(token)
                .post()
                .uri("/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateRoomRequest("Room " + uniqueSuffix(), type, "a room", null))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(RoomDetail.class)
                .returnResult()
                .getResponseBody();
    }
}
