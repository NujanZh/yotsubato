package io.github.nujanzh.yotsubato.dto.auth;

public record AuthResponse(String accessToken, String refreshToken, UserInfo userInfo) {}
