package com.livecomerce.live.domain;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

/**
 * The seller has no store, or it is deactivated, suspended or temporarily closed, so
 * catalog products cannot be sold during their lives (hot products still can).
 * 409: the request is valid but conflicts with the current state of the seller's store.
 */
public class SellerStoreNotOpenException extends DomainException {

    public SellerStoreNotOpenException(UUID sellerId) {
        super("Seller " + sellerId + " has no store open for sales; catalog products require an active store");
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/seller-store-not-open");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
