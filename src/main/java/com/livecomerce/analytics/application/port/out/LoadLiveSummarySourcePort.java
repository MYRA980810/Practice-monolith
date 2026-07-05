package com.livecomerce.analytics.application.port.out;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads the raw source data (shared {@code lives}/{@code orders}/{@code
 * order_items} tables) needed to build a {@link com.livecomerce.analytics.domain.LiveSummary}
 * snapshot. Kept separate from {@link SaveLiveSummaryPort}/{@link LoadLiveSummaryPort},
 * which deal with the persisted summary itself, not its source data.
 */
public interface LoadLiveSummarySourcePort {

    Optional<LiveSnapshot> findLiveSnapshot(UUID liveId);

    List<QualifyingOrder> findQualifyingOrders(UUID liveId);

    record LiveSnapshot(UUID storeId, OffsetDateTime startedAt, OffsetDateTime endedAt, int peakViewers) {}

    record QualifyingOrder(UUID orderId, UUID buyerId, String itemNames, BigDecimal orderTotal) {}
}
