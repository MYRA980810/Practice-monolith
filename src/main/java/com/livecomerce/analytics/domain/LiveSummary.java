package com.livecomerce.analytics.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Frozen performance snapshot of a live captured when it ends. Stays stable
 * regardless of later mutations to the underlying orders.
 */
@Entity
@Table(name = "live_summaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LiveSummary {

    @Id
    @Column(name = "live_id")
    private UUID liveId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "duration_seconds", nullable = false)
    private long durationSeconds;

    @Column(name = "peak_viewers", nullable = false)
    private int peakViewers;

    @Column(name = "total_sales", nullable = false)
    private BigDecimal totalSales;

    @Column(name = "order_count", nullable = false)
    private int orderCount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "units_sold")
    private Integer unitsSold;

    @Column(name = "total_allocated")
    private Integer totalAllocated;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "liveSummary", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<LiveSummaryOrder> orders = new ArrayList<>();

    public static LiveSummary create(UUID liveId, UUID sellerId, UUID storeId,
                                      OffsetDateTime startedAt, OffsetDateTime endedAt,
                                      long durationSeconds, int peakViewers) {
        var summary = new LiveSummary();
        summary.liveId          = liveId;
        summary.sellerId        = sellerId;
        summary.storeId         = storeId;
        summary.startedAt       = startedAt;
        summary.endedAt         = endedAt;
        summary.durationSeconds = durationSeconds;
        summary.peakViewers     = peakViewers;
        summary.totalSales      = BigDecimal.ZERO;
        summary.orderCount      = 0;
        summary.createdAt       = OffsetDateTime.now();
        return summary;
    }

    public void addOrder(UUID orderId, UUID buyerId, String itemNames, BigDecimal orderTotal) {
        this.orders.add(LiveSummaryOrder.create(this, orderId, buyerId, itemNames, orderTotal));
    }

    public void finalizeTotals(BigDecimal totalSales, int orderCount, String currency,
                                int unitsSold, long totalAllocated) {
        this.totalSales = totalSales;
        this.orderCount = orderCount;
        this.currency = currency;
        this.unitsSold = unitsSold;
        this.totalAllocated = (int) totalAllocated;
    }

    public List<LiveSummaryOrder> getOrders() {
        return Collections.unmodifiableList(orders);
    }
}
