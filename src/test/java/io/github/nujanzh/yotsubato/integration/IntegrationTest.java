package io.github.nujanzh.yotsubato.integration;

import io.github.nujanzh.yotsubato.TestcontainersConfiguration;
import io.github.nujanzh.yotsubato.dto.auth.AuthResponse;
import io.github.nujanzh.yotsubato.dto.auth.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(
        properties = {
            "spring.data.redis.host=localhost",
            "spring.data.redis.port=6379",
            "spring.data.redis.password="
        })
public abstract class IntegrationTest {

    @LocalServerPort protected int port;

    protected RestTestClient client;

    @BeforeEach
    void initClient() {
        client = RestTestClient.bindToServer().baseUrl(baseUrl()).build();
    }

    protected String baseUrl() {
        return "http://localhost:" + port + "/api";
    }

    public RestTestClient authedClient(String token) {
        return RestTestClient.bindToServer()
                .baseUrl(baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
    }

    protected record TestUser(
            UUID id,
            String username,
            String email,
            String password,
            String accessToken,
            String refreshToken) {}

    protected TestUser registerUser() {
        String suffix = uniqueSuffix();
        String username = "user_" + suffix;
        String email = suffix + "@test.io";
        String password = "Passw0rd!";

        AuthResponse auth =
                client.post()
                        .uri("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new RegisterRequest(username, password, email))
                        .exchange()
                        .expectStatus()
                        .isCreated()
                        .expectBody(AuthResponse.class)
                        .returnResult()
                        .getResponseBody();

        return new TestUser(
                auth.userInfo().id(),
                username,
                email,
                password,
                auth.accessToken(),
                auth.refreshToken());
    }

    protected String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
