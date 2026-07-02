package com.livecomerce.notification.api;

import com.livecomerce.notification.domain.NotificationNotFoundException;
import com.livecomerce.notification.domain.NotificationNotOwnedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@SuppressWarnings("null")
@RestControllerAdvice(basePackages = "com.livecomerce.notification")
class NotificationExceptionHandler {

    @ExceptionHandler(NotificationNotFoundException.class)
    ProblemDetail handleNotFound(NotificationNotFoundException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/notification-not-found"));
        return detail;
    }

    @ExceptionHandler(NotificationNotOwnedException.class)
    ProblemDetail handleNotOwned(NotificationNotOwnedException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/notification-not-owned"));
        return detail;
    }
}
