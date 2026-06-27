package com.livecomerce.analytics.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record LivePerformance(
        UUID liveId,
        String title,
        BigDecimal revenue,
        long unitsSold,
        int peakViewers,
        BigDecimal revenuePerViewer,
        double conversionRate
) {}
