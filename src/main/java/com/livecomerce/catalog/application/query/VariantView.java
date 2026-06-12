package com.livecomerce.catalog.application.query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VariantView(
        UUID id,
        UUID productId,
        String sku,
        BigDecimal priceOverride,
        BigDecimal effectivePrice,
        boolean isDefault,
        int position,
        List<OptionValueInfo> options,
        StockInfo stock
) {
    public record OptionValueInfo(String optionName, String value) {}

    public record StockInfo(int totalQuantity, int availableQuantity, int reservedQuantity) {}
}
