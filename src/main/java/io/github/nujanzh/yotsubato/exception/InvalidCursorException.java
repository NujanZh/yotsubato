package io.github.nujanzh.yotsubato.exception;

import org.springframework.http.HttpStatus;

public class InvalidCursorException extends DomainException {
    public InvalidCursorException(String token) {
        super(
                HttpStatus.BAD_REQUEST,
                "Invalid cursor",
                "The pagination cursor is invalid or malformed",
                "Invalid cursor: " + token);
    }
}
