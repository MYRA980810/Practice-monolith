package com.livecomerce.catalog.application.port.in;

import com.livecomerce.catalog.domain.Product;

import java.math.BigDecimal;
import java.util.UUID;

public interface CreateProductUseCase {

    record CreateProductCommand(
            UUID storeId,
            String name,
            String description,
            BigDecimal basePrice,
            String currency,
            String sku,
            UUID categoryId
    ) {}

    Product create(CreateProductCommand command);
}
