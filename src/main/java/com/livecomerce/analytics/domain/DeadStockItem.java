package com.livecomerce.analytics.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DeadStockItem(
        UUID productId,
        String productName,
        int availableQuantity,
        BigDecimal stockValue,
        OffsetDateTime lastSoldAt
) {}
