package com.livecomerce.catalog.application.port.in;

import com.livecomerce.catalog.application.query.VariantView;

import java.util.UUID;

public interface AddStockUseCase {

    record AddStockCommand(UUID variantId, UUID storeId, int quantity) {}

    VariantView addStock(AddStockCommand command);
}
