package com.livecomerce.analytics.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LiveSummaryTest {

    @Test
    void create_assignsAllFieldsAndZeroesAggregates() {
        var liveId    = UUID.randomUUID();
        var sellerId  = UUID.randomUUID();
        var storeId   = UUID.randomUUID();
        var startedAt = OffsetDateTime.now().minusHours(1);
        var endedAt   = OffsetDateTime.now();

        var summary = LiveSummary.create(liveId, sellerId, storeId, startedAt, endedAt, 3600L, 42);

        assertThat(summary.getLiveId()).isEqualTo(liveId);
        assertThat(summary.getSellerId()).isEqualTo(sellerId);
        assertThat(summary.getStoreId()).isEqualTo(storeId);
        assertThat(summary.getStartedAt()).isEqualTo(startedAt);
        assertThat(summary.getEndedAt()).isEqualTo(endedAt);
        assertThat(summary.getDurationSeconds()).isEqualTo(3600L);
        assertThat(summary.getPeakViewers()).isEqualTo(42);
        assertThat(summary.getTotalSales()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getOrderCount()).isZero();
        assertThat(summary.getOrders()).isEmpty();
    }

    @Test
    void addOrder_appendsChildOrderLinkedToParent() {
        var summary = LiveSummary.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.now().minusHours(1), OffsetDateTime.now(), 3600L, 10);
        var orderId   = UUID.randomUUID();
        var buyerId   = UUID.randomUUID();

        summary.addOrder(orderId, buyerId, "T-Shirt, Mug", new BigDecimal("150.00"));

        assertThat(summary.getOrders()).hasSize(1);
        var order = summary.getOrders().get(0);
        assertThat(order.getOrderId()).isEqualTo(orderId);
        assertThat(order.getBuyerId()).isEqualTo(buyerId);
        assertThat(order.getItemNames()).isEqualTo("T-Shirt, Mug");
        assertThat(order.getOrderTotal()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(order.getLiveSummary()).isSameAs(summary);
    }

    @Test
    void finalizeTotals_overwritesTotalSalesAndOrderCount() {
        var summary = LiveSummary.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.now().minusHours(1), OffsetDateTime.now(), 3600L, 10);

        summary.finalizeTotals(new BigDecimal("250.00"), 2);

        assertThat(summary.getTotalSales()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(summary.getOrderCount()).isEqualTo(2);
    }
}
