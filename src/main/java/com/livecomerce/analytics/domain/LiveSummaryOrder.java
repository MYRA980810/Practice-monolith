package com.livecomerce.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Per-order detail row of a frozen {@link LiveSummary}: one row per PAID
 * order that belonged to the live.
 */
@Entity
@Table(name = "live_summary_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LiveSummaryOrder {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_id", nullable = false)
    private LiveSummary liveSummary;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "item_names")
    private String itemNames;

    @Column(name = "order_total", nullable = false)
    private BigDecimal orderTotal;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    static LiveSummaryOrder create(LiveSummary liveSummary, UUID orderId, UUID buyerId,
                                    String itemNames, BigDecimal orderTotal) {
        var order = new LiveSummaryOrder();
        order.id          = UUID.randomUUID();
        order.liveSummary  = liveSummary;
        order.orderId     = orderId;
        order.buyerId     = buyerId;
        order.itemNames   = itemNames;
        order.orderTotal  = orderTotal;
        order.createdAt   = OffsetDateTime.now();
        return order;
    }
}
