package com.livecomerce.catalog.application.port.in;

import com.livecomerce.catalog.domain.ProductVariant;

import java.util.UUID;

public interface AddStockUseCase {

    record AddStockCommand(UUID variantId, int quantity) {}

    ProductVariant addStock(AddStockCommand command);
}
