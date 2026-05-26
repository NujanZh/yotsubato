package io.github.nujanzh.yotsubato.exception;

import org.springframework.http.HttpStatus;

public class InvalidMemberException extends DomainException {
    public InvalidMemberException(String message) {
        super(
                HttpStatus.BAD_REQUEST,
                "Invalid Member",
                "User is not a member of this room",
                message);
    }
}
