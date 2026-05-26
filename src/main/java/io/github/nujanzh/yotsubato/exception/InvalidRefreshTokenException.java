package io.github.nujanzh.yotsubato.exception;

import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends DomainException {
    public InvalidRefreshTokenException(String message) {
        super(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid refresh token", message);
    }
}
