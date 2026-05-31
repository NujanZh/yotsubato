package io.github.nujanzh.yotsubato.integration;

import io.github.nujanzh.yotsubato.dto.joinrequest.JoinRequestResponse;
import io.github.nujanzh.yotsubato.dto.joinrequest.RejectJoinRequest;
import io.github.nujanzh.yotsubato.dto.member.AddMemberRequest;
import io.github.nujanzh.yotsubato.dto.member.MemberInfo;
import io.github.nujanzh.yotsubato.dto.room.CreateRoomRequest;
import io.github.nujanzh.yotsubato.dto.room.RoomDetail;
import io.github.nujanzh.yotsubato.model.joinrequest.JoinRequestStatus;
import io.github.nujanzh.yotsubato.model.room.RoomType;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JoinRequestIntegrationTest extends IntegrationTest {
    @Test
    void requestJoin_privateRoom_returns201Pending() {
        TestUser admin = registerUser();
        TestUser requester = registerUser();
        UUID roomId = createPrivateRoom(admin.accessToken());

        JoinRequestResponse req =
                authedClient(requester.accessToken())
                        .post()
                        .uri("/rooms/{id}/join-requests", roomId)
                        .exchange()
                        .expectStatus()
                        .isCreated()
                        .expectBody(JoinRequestResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertThat(req.status()).isEqualTo(JoinRequestStatus.PENDING);
        assertThat(req.userId()).isEqualTo(requester.id());
        assertThat(req.roomId()).isEqualTo(roomId);
    }

    @Test
    void requestJoin_duplicate_returns409() {
        TestUser admin = registerUser();
        TestUser requester = registerUser();
        UUID roomId = createPrivateRoom(admin.accessToken());

        requestJoin(requester.accessToken(), roomId);

        authedClient(requester.accessToken())
                .post()
                .uri("/rooms/{id}/join-requests", roomId)
                .exchange()
                .expectStatus()
                .isEqualTo(409);
    }

    @Test
    void listJoinRequests_asAdmin_returnsPending() {
        TestUser admin = registerUser();
        TestUser requester = registerUser();
        UUID roomId = createPrivateRoom(admin.accessToken());
        JoinRequestResponse req = requestJoin(requester.accessToken(), roomId);

        JoinRequestResponse[] pending =
                authedClient(admin.accessToken())
                        .get()
                        .uri("/rooms/{id}/join-requests", roomId)
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(JoinRequestResponse[].class)
                        .returnResult()
                        .getResponseBody();

        assertThat(pending).extracting(JoinRequestResponse::id).contains(req.id());
    }

    @Test
    void listJoinRequests_asNonAdminMember_returns403() {
        TestUser admin = registerUser();
        TestUser member = registerUser();
        UUID roomId = createPrivateRoom(admin.accessToken());

        authedClient(admin.accessToken())
                .post()
                .uri("/rooms/{id}/members", roomId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AddMemberRequest(member.id()))
                .exchange()
                .expectStatus()
                .isCreated();

        authedClient(member.accessToken())
                .get()
                .uri("/rooms/{id}/join-requests", roomId)
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void listJoinRequests_isOutsider_returns404() {
        TestUser admin = registerUser();
        TestUser outsider = registerUser();
        UUID roomId = createPrivateRoom(admin.accessToken());

        authedClient(outsider.accessToken())
                .get()
                .uri("/rooms/{id}/join-requests", roomId)
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void approveJoinRequest_asAdmin_returns201AndAddsMember() {
        TestUser admin = registerUser();
        TestUser requester = registerUser();
        UUID roomId = createPrivateRoom(admin.accessToken());
        JoinRequestResponse req = requestJoin(requester.accessToken(), roomId);

        MemberInfo member =
                authedClient(admin.accessToken())
                        .post()
                        .uri("/rooms/{id}/join-requests/{reqId}/approve", roomId, req.id())
                        .exchange()
                        .expectStatus()
                        .isCreated()
                        .expectBody(MemberInfo.class)
                        .returnResult()
                        .getResponseBody();

        assertThat(member.userId()).isEqualTo(requester.id());
    }

    @Test
    void rejectJoinRequest_asAdmin_returns200WithReason() {
        TestUser admin = registerUser();
        TestUser requester = registerUser();
        UUID roomId = createPrivateRoom(admin.accessToken());
        JoinRequestResponse req = requestJoin(requester.accessToken(), roomId);

        JoinRequestResponse rejected =
                authedClient(admin.accessToken())
                        .post()
                        .uri("/rooms/{id}/join-requests/{reqId}/reject", roomId, req.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new RejectJoinRequest("not accepting new members"))
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(JoinRequestResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertThat(rejected.status()).isEqualTo(JoinRequestStatus.REJECTED);
        assertThat(rejected.rejectionReason()).isEqualTo("not accepting new members");
    }

    @Test
    void approveJoinRequest_unknownReqId_returns404() {
        TestUser admin = registerUser();
        UUID roomId = createPrivateRoom(admin.accessToken());

        authedClient(admin.accessToken())
                .post()
                .uri("/rooms/{id}/join-requests/{reqId}/approve", roomId, UUID.randomUUID())
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void cancelJoinRequest_asRequester_returns200() {
        TestUser admin = registerUser();
        TestUser requester = registerUser();
        UUID roomId = createPrivateRoom(admin.accessToken());
        JoinRequestResponse req = requestJoin(requester.accessToken(), roomId);

        authedClient(requester.accessToken())
                .delete()
                .uri("/rooms/{id}/join-requests/{reqId}", roomId, req.id())
                .exchange()
                .expectStatus()
                .isOk();
    }

    private UUID createPrivateRoom(String token) {
        return authedClient(token)
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
                .getResponseBody()
                .id();
    }

    private JoinRequestResponse requestJoin(String token, UUID roomId) {
        return authedClient(token)
                .post()
                .uri("/rooms/{id}/join-requests", roomId)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(JoinRequestResponse.class)
                .returnResult()
                .getResponseBody();
    }
}
