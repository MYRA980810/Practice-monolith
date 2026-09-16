package com.livecomerce.cart.api;

import jakarta.validation.ConstraintViolationException;
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

    /**
     * {@code @RequestParam}-level bean validation (e.g. {@code increment}/
     * {@code decrement}'s {@code delta} bound) is enforced via {@code
     * @Validated} on {@link CartController}, which routes through an AOP
     * proxy ({@code MethodValidationInterceptor}) rather than Spring MVC's
     * newer {@code HandlerMethodValidationException} path — so it surfaces
     * as a raw {@link ConstraintViolationException} that would otherwise be
     * an unhandled 500.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        detail.setType(URI.create("https://livecomerce.com/errors/cart-invalid-argument"));
        return detail;
    }
}
