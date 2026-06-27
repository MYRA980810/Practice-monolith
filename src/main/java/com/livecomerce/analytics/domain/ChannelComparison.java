package com.livecomerce.analytics.domain;

import java.math.BigDecimal;

public record ChannelComparison(
        BigDecimal storeRevenue,
        long storeOrders,
        BigDecimal storeAov,
        BigDecimal liveRevenue,
        long liveOrders,
        BigDecimal liveAov
) {}
