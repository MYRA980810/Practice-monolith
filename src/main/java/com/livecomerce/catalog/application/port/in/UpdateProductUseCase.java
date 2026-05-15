package com.livecomerce.catalog.application.port.in;

import com.livecomerce.catalog.domain.Product;

import java.math.BigDecimal;
import java.util.UUID;

public interface UpdateProductUseCase {

    record UpdateProductCommand(
            UUID productId,
            UUID storeId,
            String name,
            String description,
            BigDecimal basePrice,
            String currency,
            String sku
    ) {}

    Product update(UpdateProductCommand command);
}
