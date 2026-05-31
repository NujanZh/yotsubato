package io.github.nujanzh.yotsubato.integration;

import io.github.nujanzh.yotsubato.dto.auth.AuthResponse;
import io.github.nujanzh.yotsubato.dto.auth.LoginRequest;
import io.github.nujanzh.yotsubato.dto.auth.RefreshRequest;
import io.github.nujanzh.yotsubato.dto.auth.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthFlowIntegrationTest extends IntegrationTest {

    @Test
    void register_withValidPayload_returns201AndTokens() {
        RegisterRequest body =
                new RegisterRequest(
                        "user_" + uniqueSuffix(), "Passw0rd!", uniqueSuffix() + "@test.io");

        AuthResponse auth =
                client.post()
                        .uri("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .exchange()
                        .expectStatus()
                        .isCreated()
                        .expectBody(AuthResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertThat(auth).isNotNull();
        assertThat(auth.accessToken()).isNotBlank();
        assertThat(auth.refreshToken()).isNotBlank();
        assertThat(auth.userInfo().id()).isNotNull();
        assertThat(auth.userInfo().username()).isEqualTo(body.username());
    }

    @Test
    void register_duplicateEmail_returns409() {
        TestUser existing = registerUser();

        RegisterRequest dup =
                new RegisterRequest("user_" + uniqueSuffix(), "Passw0rd!", existing.email());

        client.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dup)
                .exchange()
                .expectStatus()
                .isEqualTo(409);
    }

    @Test
    void register_invalidPassword_returns400WithValidationErrors() {
        RegisterRequest body =
                new RegisterRequest("user_" + uniqueSuffix(), "weak", uniqueSuffix() + "@test.io");

        client.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.errors[?(@.field=='password')]")
                .exists();
    }

    @Test
    void register_invalidUsernamePattern_returns400() {
        RegisterRequest body =
                new RegisterRequest("bad name!", "Passw0rd!", uniqueSuffix() + "@test.io");

        client.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.errors[?(@.field=='username')]")
                .exists();
    }

    @Test
    void register_invalidEmail_returns400() {
        RegisterRequest body =
                new RegisterRequest("user_" + uniqueSuffix(), "Passw0rd!", "not-an-email");

        client.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.errors[?(@.field=='email')]")
                .exists();
    }

    @Test
    void register_thenLogin_returnsFreshTokens() {
        TestUser user = registerUser();

        AuthResponse login =
                client.post()
                        .uri("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new LoginRequest(user.password(), user.email()))
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(AuthResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertThat(login).isNotNull();
        assertThat(login.accessToken()).isNotBlank();
        assertThat(login.refreshToken()).isNotEqualTo(user.refreshToken());
    }

    @Test
    void login_wrongPassword_returns401() {
        TestUser user = registerUser();

        client.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest("WrongPass1!", user.email()))
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void refresh_withValidToken_rotatesAndReturns200() {
        TestUser user = registerUser();

        AuthResponse refreshed =
                client.post()
                        .uri("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new RefreshRequest(user.refreshToken()))
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(AuthResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertThat(refreshed).isNotNull();
        assertThat(refreshed.refreshToken()).isNotEqualTo(user.refreshToken());
    }

    @Test
    void refresh_reusedToken_returns401() {
        TestUser user = registerUser();

        client.post()
                .uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RefreshRequest(user.refreshToken()))
                .exchange()
                .expectStatus()
                .isOk();

        client.post()
                .uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RefreshRequest(user.refreshToken()))
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() {
        client.get().uri("/rooms").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void protectedEndpoint_withMalformedToken_returns401() {
        authedClient("not-a-real-token")
                .get()
                .uri("/rooms")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }
}
