package com.livecomerce.analytics.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record TopProduct(
        UUID productId,
        String productName,
        long unitsSold,
        BigDecimal totalRevenue
) {}
