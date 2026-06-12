package com.livecomerce.catalog.application.port.in;

import com.livecomerce.catalog.application.query.VariantView;

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

    VariantView createVariant(CreateProductVariantCommand command);
}
