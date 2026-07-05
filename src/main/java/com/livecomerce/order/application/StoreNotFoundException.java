package com.livecomerce.order.application;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

/**
 * Order-module-local equivalent of {@code store.application.StoreNotFoundException}.
 * Duplicated rather than imported to avoid a module dependency cycle (see
 * {@link com.livecomerce.order.application.port.out.LoadStoreIdPort}).
 */
public class StoreNotFoundException extends DomainException {

    public StoreNotFoundException(UUID userId) {
        super("No store found for user: " + userId);
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/store-not-found");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
