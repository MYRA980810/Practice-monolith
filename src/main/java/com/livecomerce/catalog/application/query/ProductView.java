package com.livecomerce.catalog.application.query;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProductView(
        UUID id,
        UUID storeId,
        String name,
        String description,
        BigDecimal basePrice,
        String currency,
        String sku,
        boolean active,
        boolean paused,
        UUID categoryId,
        String categoryName,
        StockInfo stock,
        List<ImageInfo> images,
        List<OptionInfo> options,
        List<VariantView> variants,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public record StockInfo(int totalQuantity, int availableQuantity, int reservedQuantity) {}

    public record ImageInfo(UUID id, String url, int position, boolean primary) {}

    public record OptionInfo(UUID id, String name, List<String> values) {}
}
