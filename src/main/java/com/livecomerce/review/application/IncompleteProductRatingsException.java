package com.livecomerce.review.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

public class IncompleteProductRatingsException extends DomainException {

    public IncompleteProductRatingsException(UUID orderId) {
        super("Product ratings must cover exactly the product items of order %s".formatted(orderId));
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/incomplete-product-ratings");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
