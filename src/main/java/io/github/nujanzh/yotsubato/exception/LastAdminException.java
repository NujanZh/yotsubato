package io.github.nujanzh.yotsubato.exception;

import org.springframework.http.HttpStatus;

public class LastAdminException extends DomainException {
    public LastAdminException(String message) {
        super(
                HttpStatus.BAD_REQUEST,
                "Admin Constraint Violation",
                "Last admin cannot leave the room",
                message);
    }
}
