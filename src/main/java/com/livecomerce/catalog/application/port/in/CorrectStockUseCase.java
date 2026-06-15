package com.livecomerce.catalog.application.port.in;

import com.livecomerce.catalog.application.query.VariantView;

import java.util.UUID;

public interface CorrectStockUseCase {

    record CorrectStockCommand(UUID variantId, UUID storeId, int availableQuantity) {}

    VariantView correctStock(CorrectStockCommand command);
}
