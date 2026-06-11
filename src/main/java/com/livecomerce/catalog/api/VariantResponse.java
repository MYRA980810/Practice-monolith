package com.livecomerce.catalog.api;

import com.livecomerce.catalog.domain.ProductVariant;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VariantResponse(
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

    public static VariantResponse from(ProductVariant v, BigDecimal basePrice) {
        var optionValues = v.getOptionValues().stream()
                .map(ov -> new OptionValueInfo(ov.getOption().getName(), ov.getValue()))
                .toList();
        var s = v.getStock();
        return new VariantResponse(
                v.getId(),
                v.getProduct().getId(),
                v.getSku(),
                v.getPriceOverride(),
                v.effectivePrice(basePrice),
                v.isDefault(),
                v.getPosition(),
                optionValues,
                new StockInfo(s.getTotalQuantity(), s.getAvailableQuantity(), s.getReservedQuantity())
        );
    }
}
