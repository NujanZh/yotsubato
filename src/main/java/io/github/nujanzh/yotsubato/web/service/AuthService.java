package io.github.nujanzh.yotsubato.web.service;

import io.github.nujanzh.yotsubato.dto.auth.AuthResponse;
import io.github.nujanzh.yotsubato.dto.auth.LoginRequest;
import io.github.nujanzh.yotsubato.dto.auth.RegisterRequest;
import io.github.nujanzh.yotsubato.exception.InvalidRefreshTokenException;
import io.github.nujanzh.yotsubato.mapper.UserMapper;
import io.github.nujanzh.yotsubato.model.user.User;
import io.github.nujanzh.yotsubato.security.RedisRefreshTokenService;
import io.github.nujanzh.yotsubato.security.jwt.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final RedisRefreshTokenService refreshTokenService;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            JwtService jwtService,
            RedisRefreshTokenService refreshTokenService,
            UserService userService,
            AuthenticationManager authenticationManager) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        User user = userService.register(request.email(), request.username(), request.password());
        return issueTokens(user);
    }

    // had a DB roundtrip here
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userService.getByEmail(request.email());
        return issueTokens(user);
    }

    public AuthResponse refresh(String oldRefreshToken) {
        UUID userId =
                refreshTokenService
                        .deleteAndReturnUserId(oldRefreshToken)
                        .orElseThrow(
                                () -> new InvalidRefreshTokenException("Invalid refresh token"));

        User user = userService.getById(userId);
        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken();
        refreshTokenService.store(refreshToken, user.getId());
        return new AuthResponse(accessToken, refreshToken, UserMapper.mapToUserInfo(user));
    }
}
