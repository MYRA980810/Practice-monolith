package com.livecomerce.live.api;

import com.livecomerce.live.domain.InvalidLiveStateException;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotLiveException;
import com.livecomerce.live.domain.LiveNotOwnedBySellerException;
import com.livecomerce.live.domain.LiveProductAlreadySoldException;
import com.livecomerce.live.domain.LiveProductNotFoundException;
import com.livecomerce.live.domain.LiveProductOutOfStockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@SuppressWarnings("null") // URI.create() is never null; ProblemDetail.setType expects @NonNull
@RestControllerAdvice(basePackages = "com.livecomerce.live")
class LiveExceptionHandler {

    @ExceptionHandler(LiveNotFoundException.class)
    ProblemDetail handleNotFound(LiveNotFoundException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/live-not-found"));
        return detail;
    }

    @ExceptionHandler(InvalidLiveStateException.class)
    ProblemDetail handleInvalidState(InvalidLiveStateException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/invalid-live-state"));
        return detail;
    }

    @ExceptionHandler(LiveNotOwnedBySellerException.class)
    ProblemDetail handleNotOwned(LiveNotOwnedBySellerException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/live-not-owned"));
        return detail;
    }

    @ExceptionHandler(LiveProductOutOfStockException.class)
    ProblemDetail handleOutOfStock(LiveProductOutOfStockException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/live-product-out-of-stock"));
        return detail;
    }

    @ExceptionHandler(LiveProductNotFoundException.class)
    ProblemDetail handleProductNotFound(LiveProductNotFoundException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/live-product-not-found"));
        return detail;
    }

    @ExceptionHandler(LiveNotLiveException.class)
    ProblemDetail handleNotLive(LiveNotLiveException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/live-not-active"));
        return detail;
    }

    @ExceptionHandler(LiveProductAlreadySoldException.class)
    ProblemDetail handleAlreadySold(LiveProductAlreadySoldException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/live-product-already-sold"));
        return detail;
    }
}
