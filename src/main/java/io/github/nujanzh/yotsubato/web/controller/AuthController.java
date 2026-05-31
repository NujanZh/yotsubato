package io.github.nujanzh.yotsubato.web.controller;

import io.github.nujanzh.yotsubato.dto.auth.AuthResponse;
import io.github.nujanzh.yotsubato.dto.auth.LoginRequest;
import io.github.nujanzh.yotsubato.dto.auth.RefreshRequest;
import io.github.nujanzh.yotsubato.dto.auth.RegisterRequest;
import io.github.nujanzh.yotsubato.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public AuthResponse register(@RequestBody @Valid RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }
}
