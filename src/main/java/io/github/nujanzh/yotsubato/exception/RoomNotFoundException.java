package io.github.nujanzh.yotsubato.exception;

public class RoomNotFoundException extends ResourceNotFoundException {
    public RoomNotFoundException(String message) {
        super("Room", message);
    }
}
