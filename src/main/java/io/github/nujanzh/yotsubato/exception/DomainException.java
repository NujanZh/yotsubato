package io.github.nujanzh.yotsubato.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DomainException extends RuntimeException {

    private final HttpStatus status;
    private final String title;
    private final String detail;

    public DomainException(HttpStatus status, String title, String detail, String message) {
        super(message);
        this.status = status;
        this.title = title;
        this.detail = detail;
    }
}
