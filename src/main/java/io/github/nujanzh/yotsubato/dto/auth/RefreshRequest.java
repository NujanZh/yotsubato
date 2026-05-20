package io.github.nujanzh.yotsubato.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "Refresh token must not be empty") String refreshToken) {}
