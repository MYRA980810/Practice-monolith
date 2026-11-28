package com.livecomerce.live.domain;

import com.livecomerce.shared.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;

/**
 * A catalog product referenced in the request body cannot be added to the live.
 * 422 (like {@link CategoryNotAvailableException}): the referenced entity is unusable, not
 * the addressed resource. A missing product and another seller's product produce the same
 * response so other stores' catalogs cannot be probed.
 */
public class CatalogProductNotAvailableException extends DomainException {

    private CatalogProductNotAvailableException(String message) {
        super(message);
    }

    public static CatalogProductNotAvailableException notInSellerStore(UUID productId, UUID variantId) {
        return new CatalogProductNotAvailableException(
                "Product " + productId + " (variant " + variantId + ") does not exist in your store");
    }

    public static CatalogProductNotAvailableException notSellable(UUID productId) {
        return new CatalogProductNotAvailableException(
                "Product " + productId + " is paused or deactivated and not available for sale");
    }

    @Override
    public URI getType() {
        return URI.create("https://livecomerce.com/errors/catalog-product-not-available");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
