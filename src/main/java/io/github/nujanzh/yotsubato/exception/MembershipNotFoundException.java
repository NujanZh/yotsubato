package io.github.nujanzh.yotsubato.exception;

public class MembershipNotFoundException extends ResourceNotFoundException {
    public MembershipNotFoundException(String message) {
        super("Membership", message);
    }
}
