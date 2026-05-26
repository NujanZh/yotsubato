package io.github.nujanzh.yotsubato.exception;

public class JoinRequestNotFoundException extends ResourceNotFoundException {
    public JoinRequestNotFoundException(String message) {
        super("Join Request", message);
    }
}
