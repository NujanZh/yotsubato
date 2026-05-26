package io.github.nujanzh.yotsubato.exception;

import org.springframework.http.HttpStatus;

public class DirectRoomOperationException extends DomainException {
    public DirectRoomOperationException(String message) {
        super(
                HttpStatus.BAD_REQUEST,
                "Invalid Room Operation",
                "Direct room operation failed",
                message);
    }
}
