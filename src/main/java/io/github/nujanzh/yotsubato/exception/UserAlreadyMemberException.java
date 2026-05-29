package io.github.nujanzh.yotsubato.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyMemberException extends DomainException {
    public UserAlreadyMemberException(String message) {
        super(HttpStatus.CONFLICT, "User Already Member", message, message);
    }
}
