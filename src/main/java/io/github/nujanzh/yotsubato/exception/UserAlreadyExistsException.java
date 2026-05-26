package io.github.nujanzh.yotsubato.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends DomainException {
    public UserAlreadyExistsException(String message) {
        super(HttpStatus.CONFLICT, "Conflict", "Registration failed", message);
    }
}
