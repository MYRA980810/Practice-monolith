package com.livecomerce.live.application.port.in;

import com.livecomerce.live.domain.LiveProduct;

import java.math.BigDecimal;
import java.util.UUID;

public interface AddCatalogProductUseCase {

    LiveProduct addCatalogProduct(AddCatalogProductCommand command);

    record AddCatalogProductCommand(
            UUID liveId,
            UUID sellerId,
            UUID productId,
            UUID variantId,
            String nameSnapshot,
            BigDecimal priceSnapshot,
            String currency,
            int stockAllocated
    ) {}
}
