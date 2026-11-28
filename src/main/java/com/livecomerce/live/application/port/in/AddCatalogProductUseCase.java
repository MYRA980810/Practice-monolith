package com.livecomerce.live.application.port.in;

import com.livecomerce.live.domain.LiveProduct;

import java.util.UUID;

public interface AddCatalogProductUseCase {

    LiveProduct addCatalogProduct(AddCatalogProductCommand command);

    /**
     * Name, price and currency are not part of the command: they are snapshotted from the
     * catalog. {@code imageUrl} is an optional override of the catalog's primary image.
     */
    record AddCatalogProductCommand(
            UUID liveId,
            UUID sellerId,
            UUID productId,
            UUID variantId,
            int stockAllocated,
            String imageUrl
    ) {}
}
