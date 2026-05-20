package com.livecomerce.shared.api;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Objects;

import org.springframework.lang.NonNull;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @NonNull
    private static final URI MALFORMED_REQUEST_TYPE =
            Objects.requireNonNull(URI.create("https://livecomerce.com/errors/malformed-request"));

    @SuppressWarnings("null")
    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomain(DomainException e) {
        var detail = ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage());
        detail.setType(e.getType());
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleNotReadable(HttpMessageNotReadableException e) {
        var detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setType(MALFORMED_REQUEST_TYPE);
        detail.setDetail(resolveDetail(e));
        return detail;
    }

    private String resolveDetail(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof InvalidFormatException ife
                && ife.getTargetType() != null
                && ife.getTargetType().isEnum()) {
            return "Invalid value '%s' for field '%s'".formatted(
                    ife.getValue(),
                    ife.getPath().isEmpty() ? "unknown" : ife.getPath().getFirst().getFieldName()
            );
        }
        return "Request body is missing or malformed";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        var detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setDetail("Validation failed");
        detail.setProperty("errors", e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList());
        return detail;
    }
}
