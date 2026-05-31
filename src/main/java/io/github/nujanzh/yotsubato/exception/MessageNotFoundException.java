package io.github.nujanzh.yotsubato.exception;

public class MessageNotFoundException extends ResourceNotFoundException {
    public MessageNotFoundException(String message) {
        super("Message", message);
    }
}
