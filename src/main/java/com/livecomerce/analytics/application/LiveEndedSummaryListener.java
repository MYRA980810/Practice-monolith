package com.livecomerce.analytics.application;

import com.livecomerce.analytics.application.port.out.LoadLiveSummarySourcePort;
import com.livecomerce.analytics.application.port.out.SaveLiveSummaryPort;
import com.livecomerce.analytics.domain.LiveSummary;
import com.livecomerce.live.LiveEndedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * On {@link LiveEndedEvent}, freezes a performance snapshot (duration, peak
 * viewers, sales, order count, per-order detail) into {@code live_summaries}
 * / {@code live_summary_orders}. Idempotent against Modulith's at-least-once
 * event redelivery: first-write-wins, guarded by an existence check against
 * the {@code live_id} primary key.
 */
@Component
@RequiredArgsConstructor
public class LiveEndedSummaryListener {

    private static final Logger log = LoggerFactory.getLogger(LiveEndedSummaryListener.class);

    /**
     * Frozen snapshot default when a source order carries no currency.
     * Module-local (not extracted from order.domain.Order) to avoid an
     * analytics -> order dependency that would break the hexagonal boundary
     * kept by native/read-only cross-module access elsewhere in this module.
     */
    private static final String DEFAULT_CURRENCY = "MXN";

    private final SaveLiveSummaryPort saveLiveSummaryPort;
    private final LoadLiveSummarySourcePort loadLiveSummarySourcePort;

    @ApplicationModuleListener
    public void on(LiveEndedEvent event) {
        if (saveLiveSummaryPort.existsByLiveId(event.liveId())) {
            log.info("Live summary already exists for liveId {}, skipping redelivered event", event.liveId());
            return;
        }

        var snapshot = loadLiveSummarySourcePort.findLiveSnapshot(event.liveId())
                .orElseThrow(() -> new IllegalStateException(
                        "No live found for summary snapshot: " + event.liveId()));

        long durationSeconds = Duration.between(snapshot.startedAt(), snapshot.endedAt()).getSeconds();

        var summary = LiveSummary.create(
                event.liveId(),
                event.sellerId(),
                snapshot.storeId(),
                snapshot.startedAt(),
                snapshot.endedAt(),
                durationSeconds,
                snapshot.peakViewers());

        var qualifyingOrders = loadLiveSummarySourcePort.findQualifyingOrders(event.liveId());

        var totalSales = BigDecimal.ZERO;
        var totalUnits = 0;
        for (var order : qualifyingOrders) {
            summary.addOrder(order.orderId(), order.buyerId(), order.itemNames(), order.orderTotal());
            totalSales = totalSales.add(order.orderTotal());
            totalUnits += order.unitsSold();
        }
        var totalAllocated = loadLiveSummarySourcePort.findTotalAllocated(event.liveId());
        summary.finalizeTotals(totalSales, qualifyingOrders.size(), DEFAULT_CURRENCY, totalUnits, totalAllocated);

        saveLiveSummaryPort.save(summary);
    }
}
