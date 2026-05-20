package io.github.nujanzh.yotsubato.mapper;

import io.github.nujanzh.yotsubato.dto.auth.UserInfo;
import io.github.nujanzh.yotsubato.model.user.User;

public class UserMapper {
    public static UserInfo mapToUserInfo(User user) {
        return new UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getStatus().name());
    }
}
