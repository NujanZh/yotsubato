package io.github.nujanzh.yotsubato.web.error;

import io.github.nujanzh.yotsubato.exception.JwtValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return ProblemDetailFactory.build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred.",
                request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        log.info("Invalid credentials: {}", ex.getMessage());
        return ProblemDetailFactory.build(
                HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid credentials", request);
    }

    @ExceptionHandler(JwtValidationException.class)
    public ProblemDetail handleJwtValidationException(
            JwtValidationException ex, HttpServletRequest request) {
        log.debug("Invalid token: {}", ex.getMessage());
        return ProblemDetailFactory.build(
                HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid authentication", request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        log.debug("Request parameter validation failed: {}", ex.getMessage());
        return ProblemDetailFactory.build(
                HttpStatus.BAD_REQUEST, "Validation failed", "Request validation failed", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        HttpServletRequest httpRequest = ((ServletWebRequest) request).getRequest();

        ProblemDetail problemDetail =
                ProblemDetailFactory.build(
                        HttpStatus.BAD_REQUEST,
                        "Validation failed",
                        "One or more fields are invalid.",
                        httpRequest);

        List<ValidationError> errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(fe -> new ValidationError(fe.getField(), fe.getDefaultMessage()))
                        .toList();

        problemDetail.setProperty("errors", errors);

        log.debug("Request body validation failed: {}", ex.getMessage());
        return new ResponseEntity<>(problemDetail, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ProblemDetail createProblemDetail(
            Exception ex,
            HttpStatusCode status,
            String defaultDetail,
            String detailMessageCode,
            Object[] detailMessageArguments,
            WebRequest request) {

        HttpServletRequest httpRequest = ((ServletWebRequest) request).getRequest();

        return ProblemDetailFactory.build(
                HttpStatus.valueOf(status.value()), defaultDetail, httpRequest);
    }
}
