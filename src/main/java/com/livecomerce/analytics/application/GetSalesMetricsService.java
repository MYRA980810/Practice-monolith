package com.livecomerce.analytics.application;

import com.livecomerce.analytics.application.port.in.GetSalesMetricsUseCase;
import com.livecomerce.analytics.application.port.out.LoadSalesMetricsPort;
import com.livecomerce.analytics.domain.PaymentMethodBreakdown;
import com.livecomerce.analytics.domain.RevenueByPeriod;
import com.livecomerce.analytics.domain.SalesSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class GetSalesMetricsService implements GetSalesMetricsUseCase {

    private final LoadSalesMetricsPort loadSalesMetricsPort;

    @Override
    public SalesSummary getSummary(SalesQuery query) {
        return loadSalesMetricsPort.loadSummary(query.storeId(), query.from(), query.to());
    }

    @Override
    public List<RevenueByPeriod> getRevenueByPeriod(RevenueQuery query) {
        return loadSalesMetricsPort.loadRevenueByPeriod(
                query.storeId(), query.period(), query.from(), query.to());
    }

    @Override
    public List<PaymentMethodBreakdown> getPaymentMethodBreakdown(SalesQuery query) {
        return loadSalesMetricsPort.loadPaymentMethodBreakdown(
                query.storeId(), query.from(), query.to());
    }

    @Override
    public BigDecimal getCommission(SalesQuery query) {
        return loadSalesMetricsPort.loadCommission(query.storeId(), query.from(), query.to());
    }
}
