package com.livecomerce.cart.api;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
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

    /**
     * The cart store ({@code RedisCartStoreAdapter}) has no retry/circuit
     * breaker of its own, so a Redis timeout or connection failure would
     * otherwise surface as an unhandled 500. This net maps it to a 503 so
     * clients can distinguish "cart storage is temporarily unavailable,
     * retry" from a genuine server bug, without leaking the underlying
     * exception message (which may contain infrastructure details).
     */
    @ExceptionHandler(DataAccessException.class)
    ProblemDetail handleDataAccessException(DataAccessException e) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Cart is temporarily unavailable, please retry.");
        detail.setType(URI.create("https://livecomerce.com/errors/cart-storage-unavailable"));
        return detail;
    }
}
