package io.github.nujanzh.yotsubato.exception;

public class JwtValidationException extends RuntimeException {
    public JwtValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public JwtValidationException(String message) {
        super(message);
    }
}
