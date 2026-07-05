package com.livecomerce.analytics.api;

import com.livecomerce.analytics.domain.LiveSummary;
import com.livecomerce.analytics.domain.LiveSummaryOrder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record LiveSummaryResponse(
        UUID liveId,
        long durationSeconds,
        int peakViewers,
        BigDecimal totalSales,
        int orderCount,
        List<OrderDetail> orders
) {
    public static LiveSummaryResponse from(LiveSummary summary) {
        return new LiveSummaryResponse(
                summary.getLiveId(),
                summary.getDurationSeconds(),
                summary.getPeakViewers(),
                summary.getTotalSales(),
                summary.getOrderCount(),
                summary.getOrders().stream().map(OrderDetail::from).toList()
        );
    }

    public record OrderDetail(UUID buyerId, String itemNames, BigDecimal orderTotal) {
        static OrderDetail from(LiveSummaryOrder order) {
            return new OrderDetail(order.getBuyerId(), order.getItemNames(), order.getOrderTotal());
        }
    }
}
