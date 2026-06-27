package com.livecomerce.analytics.domain;

import java.math.BigDecimal;

public record PaymentMethodBreakdown(
        String method,
        long count,
        BigDecimal totalAmount
) {}
