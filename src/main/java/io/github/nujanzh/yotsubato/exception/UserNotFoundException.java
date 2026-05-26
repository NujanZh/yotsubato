package io.github.nujanzh.yotsubato.exception;

public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(String message) {
        super("User", message);
    }
}
