package io.github.nujanzh.yotsubato.dto.auth;

import java.util.UUID;

public record UserInfo(UUID id, String username, String email, String avatarUrl, String status) {}
