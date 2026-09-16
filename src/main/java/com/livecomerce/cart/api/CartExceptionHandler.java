package com.livecomerce.cart.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Cart's own business rejections (live-exclusive, insufficient stock,
 * quantity cap, unavailable, line-not-found) are Result records, not
 * exceptions (design D1/D8) — {@link CartController} maps those directly to
 * HTTP status. This handler exists as a defensive net for the domain layer's
 * own invariant violations ({@code Cart}/{@code CartItem}/{@code
 * CartLineKey} all throw {@link IllegalArgumentException} on invalid
 * reconstruction), which would otherwise surface as an unhandled 500.
 */
@SuppressWarnings("null") // URI.create() is never null; ProblemDetail.setType expects @NonNull
@RestControllerAdvice(basePackages = "com.livecomerce.cart")
class CartExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleInvalidArgument(IllegalArgumentException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/cart-invalid-argument"));
        return detail;
    }
}
