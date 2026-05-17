package io.github.nujanzh.yotsubato.web.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.Objects;

public final class ProblemDetailFactory {

    private ProblemDetailFactory() {}

    public static ProblemDetail build(
            HttpStatus status, String title, String detail, HttpServletRequest request) {

        Objects.requireNonNull(title, "title");

        ProblemDetail problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setTitle(title);
        problemDetail.setDetail(detail);
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        String traceId = MDC.get("traceId");

        if (traceId != null) {
            problemDetail.setProperty("traceId", traceId);
        }

        return problemDetail;
    }

    public static ProblemDetail build(
            HttpStatus status, String detail, HttpServletRequest request) {

        return build(status, status.getReasonPhrase(), detail, request);
    }
}
