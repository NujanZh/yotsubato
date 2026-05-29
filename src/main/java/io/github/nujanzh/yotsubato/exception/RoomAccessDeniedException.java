package io.github.nujanzh.yotsubato.exception;

import org.springframework.http.HttpStatus;

public class RoomAccessDeniedException extends DomainException {
    public RoomAccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, "Forbidden", message, message);
    }
}
