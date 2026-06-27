package com.livecomerce.analytics.domain;

import java.util.UUID;

public record StockAlert(
        UUID productId,
        String productName,
        UUID variantId,
        String sku,
        int availableQuantity,
        String alertType
) {}
