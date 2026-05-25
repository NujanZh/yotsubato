package io.github.nujanzh.yotsubato.exception;

public class InvalidMemberException extends RuntimeException {
    public InvalidMemberException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidMemberException(String message) {
        super(message);
    }
}
