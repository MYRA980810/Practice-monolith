package com.livecomerce.analytics.application.port.in;

import com.livecomerce.analytics.domain.PaymentMethodBreakdown;
import com.livecomerce.analytics.domain.RevenueByPeriod;
import com.livecomerce.analytics.domain.SalesSummary;
import com.livecomerce.analytics.domain.Period;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface GetSalesMetricsUseCase {

    SalesSummary getSummary(SalesQuery query);

    List<RevenueByPeriod> getRevenueByPeriod(RevenueQuery query);

    List<PaymentMethodBreakdown> getPaymentMethodBreakdown(SalesQuery query);

    BigDecimal getCommission(SalesQuery query);

    record SalesQuery(UUID storeId, OffsetDateTime from, OffsetDateTime to) {}

    record RevenueQuery(UUID storeId, Period period, OffsetDateTime from, OffsetDateTime to) {}
}
