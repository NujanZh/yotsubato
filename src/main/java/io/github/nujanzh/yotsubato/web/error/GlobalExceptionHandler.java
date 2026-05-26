package io.github.nujanzh.yotsubato.web.error;

import io.github.nujanzh.yotsubato.exception.*;
import io.github.nujanzh.yotsubato.security.jwt.JwtValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
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
        log.error("Unhandled exception at {}", request.getRequestURI(), ex);
        return ProblemDetailFactory.build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred.",
                request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ProblemDetailFactory.build(
                HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication required", request);
    }

    @ExceptionHandler(JwtValidationException.class)
    public ProblemDetail handleJwtValidationException(
            JwtValidationException ex, HttpServletRequest request) {
        log.warn("Invalid token: {}", ex.getMessage());
        return ProblemDetailFactory.build(
                HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid authentication", request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.debug("{} not found: {}", ex.getResourceName(), ex.getMessage());
        return ProblemDetailFactory.build(
                HttpStatus.NOT_FOUND, "Not Found", ex.getResourceName() + " not found", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return ProblemDetailFactory.build(
                HttpStatus.FORBIDDEN, "Forbidden", "Access denied", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ProblemDetailFactory.build(
                HttpStatus.BAD_REQUEST, "Invalid argument", ex.getMessage(), request);
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex, HttpServletRequest request) {
        log.warn("{}: {}", ex.getDetail(), ex.getMessage());
        return ProblemDetailFactory.build(ex.getStatus(), ex.getTitle(), ex.getDetail(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {

        log.debug("Request parameter validation failed: {}", ex.getMessage());

        ProblemDetail problemDetail =
                ProblemDetailFactory.build(
                        HttpStatus.BAD_REQUEST,
                        "Validation failed",
                        "Request validation failed",
                        request);

        List<ValidationError> errors =
                ex.getConstraintViolations().stream()
                        .map(
                                cv ->
                                        new ValidationError(
                                                cv.getPropertyPath().toString(), cv.getMessage()))
                        .toList();

        problemDetail.setProperty("errors", errors);

        return problemDetail;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.debug("Request body validation failed: {}", ex.getMessage());

        ProblemDetail problemDetail =
                ProblemDetailFactory.build(
                        HttpStatus.BAD_REQUEST,
                        "Validation failed",
                        "One or more fields are invalid.",
                        toHttpRequest(request));

        List<ValidationError> errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(fe -> new ValidationError(fe.getField(), fe.getDefaultMessage()))
                        .toList();

        problemDetail.setProperty("errors", errors);

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

        return ProblemDetailFactory.build(
                HttpStatus.valueOf(status.value()), defaultDetail, toHttpRequest(request));
    }

    private static HttpServletRequest toHttpRequest(WebRequest request) {
        return ((ServletWebRequest) request).getRequest();
    }
}
