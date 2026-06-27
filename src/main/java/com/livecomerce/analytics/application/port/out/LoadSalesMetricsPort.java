package com.livecomerce.analytics.application.port.out;

import com.livecomerce.analytics.domain.PaymentMethodBreakdown;
import com.livecomerce.analytics.domain.RevenueByPeriod;
import com.livecomerce.analytics.domain.SalesSummary;
import com.livecomerce.analytics.domain.Period;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface LoadSalesMetricsPort {

    SalesSummary loadSummary(UUID storeId, OffsetDateTime from, OffsetDateTime to);

    List<RevenueByPeriod> loadRevenueByPeriod(UUID storeId, Period period, OffsetDateTime from, OffsetDateTime to);

    List<PaymentMethodBreakdown> loadPaymentMethodBreakdown(UUID storeId, OffsetDateTime from, OffsetDateTime to);

    BigDecimal loadCommission(UUID storeId, OffsetDateTime from, OffsetDateTime to);
}
