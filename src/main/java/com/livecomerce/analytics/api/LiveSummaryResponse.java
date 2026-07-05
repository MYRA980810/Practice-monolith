package com.livecomerce.analytics.api;

import com.livecomerce.analytics.domain.ConversionRate;
import com.livecomerce.analytics.domain.LiveSummary;
import com.livecomerce.analytics.domain.LiveSummaryOrder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record LiveSummaryResponse(
        UUID liveId,
        long durationSeconds,
        int peakViewers,
        BigDecimal totalSales,
        int orderCount,
        String currency,
        Double conversionRate,
        List<OrderDetail> orders
) {
    public static LiveSummaryResponse from(LiveSummary summary, Map<UUID, String> buyerNames) {
        Double conversionRate = (summary.getUnitsSold() != null && summary.getTotalAllocated() != null)
                ? ConversionRate.compute(summary.getUnitsSold(), summary.getTotalAllocated())
                : null;

        return new LiveSummaryResponse(
                summary.getLiveId(),
                summary.getDurationSeconds(),
                summary.getPeakViewers(),
                summary.getTotalSales(),
                summary.getOrderCount(),
                summary.getCurrency(),
                conversionRate,
                summary.getOrders().stream()
                        .map(order -> OrderDetail.from(order, buyerNames.get(order.getBuyerId())))
                        .toList()
        );
    }

    public record OrderDetail(UUID orderId, UUID buyerId, String customerName, String itemNames, BigDecimal orderTotal) {
        static OrderDetail from(LiveSummaryOrder order, String customerName) {
            return new OrderDetail(order.getOrderId(), order.getBuyerId(), customerName,
                    order.getItemNames(), order.getOrderTotal());
        }
    }
}
