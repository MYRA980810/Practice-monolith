package com.livecomerce.analytics.domain;

import java.math.BigDecimal;

public record RevenueByPeriod(
        String periodLabel,
        BigDecimal revenue,
        long orderCount
) {}
