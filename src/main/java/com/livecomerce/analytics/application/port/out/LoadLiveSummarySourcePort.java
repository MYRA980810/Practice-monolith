package com.livecomerce.analytics.application.port.out;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads the raw source data (shared {@code lives}/{@code orders}/{@code
 * order_items}/{@code live_products}/{@code users} tables) needed to build a
 * {@link com.livecomerce.analytics.domain.LiveSummary} snapshot and to enrich
 * it at read time. Kept separate from {@link SaveLiveSummaryPort}/{@link LoadLiveSummaryPort},
 * which deal with the persisted summary itself, not its source data.
 */
public interface LoadLiveSummarySourcePort {

    Optional<LiveSnapshot> findLiveSnapshot(UUID liveId);

    List<QualifyingOrder> findQualifyingOrders(UUID liveId);

    /** {@code SUM(stock_allocated)} for all {@code live_products} rows of the live. */
    long findTotalAllocated(UUID liveId);

    /**
     * Batched buyer display-name lookup keyed by buyerId, resolved read-time
     * (not frozen) against the current {@code users} table. Empty input skips
     * the query. Missing/deleted users are simply absent from the result map.
     */
    Map<UUID, String> findBuyerNames(Collection<UUID> buyerIds);

    record LiveSnapshot(UUID storeId, OffsetDateTime startedAt, OffsetDateTime endedAt, int peakViewers) {}

    record QualifyingOrder(UUID orderId, UUID buyerId, String itemNames, BigDecimal orderTotal, int unitsSold) {}
}
