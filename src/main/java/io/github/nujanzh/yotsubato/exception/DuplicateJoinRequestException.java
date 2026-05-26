package io.github.nujanzh.yotsubato.exception;

import org.springframework.http.HttpStatus;

public class DuplicateJoinRequestException extends DomainException {
    public DuplicateJoinRequestException(String message) {
        super(
                HttpStatus.CONFLICT,
                "Duplicate Join Request",
                "User already has a pending request for this room",
                message);
    }
}
