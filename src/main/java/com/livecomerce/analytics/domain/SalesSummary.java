package com.livecomerce.analytics.domain;

import java.math.BigDecimal;

public record SalesSummary(
        BigDecimal totalRevenue,
        long totalOrders,
        long paidOrders,
        long cancelledOrders,
        BigDecimal averageOrderValue,
        double cancellationRate,
        double conversionRate
) {}
