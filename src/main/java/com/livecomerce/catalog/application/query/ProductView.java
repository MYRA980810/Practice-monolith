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
        BigDecimal compareAtPrice,
        String discountLabel,
        String currency,
        String sku,
        boolean active,
        boolean paused,
        UUID categoryId,
        String categoryName,
        StockInfo stock,
        String stockLabel,
        long soldCount,
        List<ImageInfo> images,
        List<OptionInfo> options,
        List<VariantView> variants,
        double averageRating,
        long reviewCount,
        boolean pinnedNow,
        boolean exclusiveToActiveLive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public record StockInfo(int totalQuantity, int availableQuantity, int reservedQuantity) {}

    public record ImageInfo(UUID id, String url, int position, boolean primary) {}

    public record OptionInfo(UUID id, String name, List<OptionValueInfo> values) {
        public record OptionValueInfo(String value, String swatchHex) {}
    }
}
