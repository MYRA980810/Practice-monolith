package com.livecomerce.catalog.application.port.in;

import java.util.UUID;

public record ProductFilter(
        UUID storeId,
        UUID categoryId,
        SortBy sortBy,
        StockLevel stockLevel
) {
    public ProductFilter {
        sortBy     = sortBy     != null ? sortBy     : SortBy.RECENTLY_ADDED;
        stockLevel = stockLevel != null ? stockLevel : StockLevel.ALL;
    }

    public enum SortBy { PRICE_ASC, PRICE_DESC, RECENTLY_ADDED }
    public enum StockLevel { ALL, CRITICAL, NORMAL }
}
