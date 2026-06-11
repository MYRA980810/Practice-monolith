package com.livecomerce.catalog.application.port.in;

import com.livecomerce.catalog.domain.ProductVariant;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface CreateProductVariantUseCase {

    record CreateProductVariantCommand(
            UUID productId,
            UUID storeId,
            Map<String, String> optionSelection,
            String sku,
            BigDecimal priceOverride
    ) {}

    ProductVariant createVariant(CreateProductVariantCommand command);
}
