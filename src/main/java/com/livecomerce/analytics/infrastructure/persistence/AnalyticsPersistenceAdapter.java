package com.livecomerce.analytics.infrastructure.persistence;

import com.livecomerce.analytics.application.port.out.LoadChannelMetricsPort;
import com.livecomerce.analytics.application.port.out.LoadProductMetricsPort;
import com.livecomerce.analytics.application.port.out.LoadSalesMetricsPort;
import com.livecomerce.analytics.application.port.out.LoadStockMetricsPort;
import com.livecomerce.analytics.domain.ChannelComparison;
import com.livecomerce.analytics.domain.DeadStockItem;
import com.livecomerce.analytics.domain.LivePerformance;
import com.livecomerce.analytics.domain.PaymentMethodBreakdown;
import com.livecomerce.analytics.domain.RevenueByPeriod;
import com.livecomerce.analytics.domain.SalesSummary;
import com.livecomerce.analytics.domain.StockAlert;
import com.livecomerce.analytics.domain.TopProduct;
import com.livecomerce.analytics.domain.Period;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AnalyticsPersistenceAdapter implements LoadSalesMetricsPort, LoadChannelMetricsPort,
        LoadProductMetricsPort, LoadStockMetricsPort {

    private final SalesMetricsRepository salesMetricsRepository;
    private final ChannelMetricsRepository channelMetricsRepository;
    private final ProductMetricsRepository productMetricsRepository;
    private final StockMetricsRepository stockMetricsRepository;

    // --- Sales ---

    @Override
    public SalesSummary loadSummary(UUID storeId, OffsetDateTime from, OffsetDateTime to) {
        Object[] row = salesMetricsRepository.findSalesSummary(storeId, from, to);

        var totalRevenue = toBigDecimal(row[0]);
        long totalOrders = toLong(row[1]);
        long paidOrders = toLong(row[2]);
        long cancelledOrders = toLong(row[3]);

        var aov = paidOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(paidOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        double cancellationRate = totalOrders > 0
                ? (double) cancelledOrders / totalOrders
                : 0.0;

        long totalItems = salesMetricsRepository.countTotalItems(storeId, from, to);
        long paidItems = salesMetricsRepository.countPaidItems(storeId, from, to);

        double conversionRate = totalItems > 0
                ? (double) paidItems / totalItems
                : 0.0;

        return new SalesSummary(totalRevenue, totalOrders, paidOrders, cancelledOrders,
                aov, cancellationRate, conversionRate);
    }

    @Override
    public List<RevenueByPeriod> loadRevenueByPeriod(UUID storeId, Period period,
                                                      OffsetDateTime from, OffsetDateTime to) {
        String periodStr = period.name().toLowerCase().replace("ly", "");
        if ("dai".equals(periodStr)) periodStr = "day";

        List<Object[]> rows = salesMetricsRepository.findRevenueByPeriod(storeId, periodStr, from, to);

        return rows.stream()
                .map(row -> new RevenueByPeriod(
                        row[0].toString(),
                        toBigDecimal(row[1]),
                        toLong(row[2])
                ))
                .toList();
    }

    @Override
    public List<PaymentMethodBreakdown> loadPaymentMethodBreakdown(UUID storeId,
                                                                     OffsetDateTime from,
                                                                     OffsetDateTime to) {
        return salesMetricsRepository.findPaymentMethodBreakdown(storeId, from, to).stream()
                .map(row -> new PaymentMethodBreakdown(
                        (String) row[0],
                        toLong(row[1]),
                        toBigDecimal(row[2])
                ))
                .toList();
    }

    @Override
    public BigDecimal loadCommission(UUID storeId, OffsetDateTime from, OffsetDateTime to) {
        Object result = salesMetricsRepository.findCommission(storeId, from, to);
        return result != null ? toBigDecimal(result) : BigDecimal.ZERO;
    }

    // --- Channels ---

    @Override
    public ChannelComparison loadChannelComparison(UUID storeId, OffsetDateTime from, OffsetDateTime to) {
        Object[] row = channelMetricsRepository.findChannelComparison(storeId, from, to);

        var storeRevenue = toBigDecimal(row[0]);
        long storeOrders = toLong(row[1]);
        var liveRevenue = toBigDecimal(row[2]);
        long liveOrders = toLong(row[3]);

        var storeAov = storeOrders > 0
                ? storeRevenue.divide(BigDecimal.valueOf(storeOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        var liveAov = liveOrders > 0
                ? liveRevenue.divide(BigDecimal.valueOf(liveOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new ChannelComparison(storeRevenue, storeOrders, storeAov,
                liveRevenue, liveOrders, liveAov);
    }

    @Override
    public List<LivePerformance> loadLivePerformances(UUID storeId, OffsetDateTime from, OffsetDateTime to) {
        return channelMetricsRepository.findLivePerformances(storeId, from, to).stream()
                .map(row -> {
                    var liveId = (UUID) row[0];
                    var title = (String) row[1];
                    var revenue = toBigDecimal(row[2]);
                    long unitsSold = toLong(row[3]);
                    int peakViewers = ((Number) row[4]).intValue();
                    long totalAllocated = toLong(row[5]);

                    var revenuePerViewer = peakViewers > 0
                            ? revenue.divide(BigDecimal.valueOf(peakViewers), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    double conversionRate = totalAllocated > 0
                            ? (double) unitsSold / totalAllocated
                            : 0.0;

                    return new LivePerformance(liveId, title, revenue, unitsSold,
                            peakViewers, revenuePerViewer, conversionRate);
                })
                .toList();
    }

    // --- Products ---

    @Override
    public List<TopProduct> loadTopProducts(UUID storeId, String metric, UUID categoryId,
                                             int limit, OffsetDateTime from, OffsetDateTime to) {
        List<Object[]> rows = "UNITS".equalsIgnoreCase(metric)
                ? productMetricsRepository.findTopByUnits(storeId, categoryId, limit, from, to)
                : productMetricsRepository.findTopByRevenue(storeId, categoryId, limit, from, to);

        return rows.stream()
                .map(row -> new TopProduct(
                        (UUID) row[0],
                        (String) row[1],
                        toLong(row[2]),
                        toBigDecimal(row[3])
                ))
                .toList();
    }

    // --- Stock ---

    @Override
    public List<DeadStockItem> loadDeadStock(UUID storeId, int notSoldInDays) {
        return stockMetricsRepository.findDeadStock(storeId, notSoldInDays).stream()
                .map(row -> new DeadStockItem(
                        (UUID) row[0],
                        (String) row[1],
                        ((Number) row[2]).intValue(),
                        toBigDecimal(row[3]),
                        toOffsetDateTime(row[4])
                ))
                .toList();
    }

    @Override
    public List<StockAlert> loadStockAlerts(UUID storeId) {
        return stockMetricsRepository.findStockAlerts(storeId).stream()
                .map(row -> new StockAlert(
                        (UUID) row[0],
                        (String) row[1],
                        (UUID) row[2],
                        (String) row[3],
                        ((Number) row[4]).intValue(),
                        (String) row[5]
                ))
                .toList();
    }

    // --- Helpers ---

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }

    private static long toLong(Object value) {
        if (value == null) return 0L;
        return ((Number) value).longValue();
    }

    private static OffsetDateTime toOffsetDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof OffsetDateTime odt) return odt;
        if (value instanceof Timestamp ts) return ts.toInstant().atOffset(ZoneOffset.UTC);
        return null;
    }
}
