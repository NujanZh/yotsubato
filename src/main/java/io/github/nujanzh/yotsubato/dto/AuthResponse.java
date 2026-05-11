package io.github.nujanzh.yotsubato.dto;

import java.util.UUID;

public record AuthResponse(String accessToken, String refreshToken, UserInfo userInfo) {
    public record UserInfo(
            UUID id, String username, String email, String avatarUrl, String status) {}
}
