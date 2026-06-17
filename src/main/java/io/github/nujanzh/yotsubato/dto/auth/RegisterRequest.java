package io.github.nujanzh.yotsubato.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username must not be empty")
                @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
                @Pattern(
                        regexp = "^[a-zA-Z0-9_]+$",
                        message = "Username can only contain letters, numbers, and underscores")
                String username,
        @NotBlank(message = "Password must not be empty")
                @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
                @Pattern(
                        regexp =
                                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                        message =
                                "Password must contain at least one uppercase letter, one lowercase"
                                        + " letter, one number, and one special character")
                String password,
        @NotBlank(message = "Email must not be empty")
                @Email(message = "Must be a valid email address")
                @Size(max = 254, message = "Email must not exceed 254 characters")
                String email) {}
