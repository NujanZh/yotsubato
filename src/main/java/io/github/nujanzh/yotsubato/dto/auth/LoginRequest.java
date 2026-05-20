package io.github.nujanzh.yotsubato.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Password must not be empty") String password,
        @NotBlank(message = "Email must not be empty")
                @Email(message = "Must be a valid email address")
                @Size(max = 254, message = "Email must not exceed 254 characters")
                String email) {}
