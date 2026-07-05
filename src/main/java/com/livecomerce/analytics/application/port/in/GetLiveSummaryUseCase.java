package com.livecomerce.analytics.application.port.in;

import com.livecomerce.analytics.domain.LiveSummary;

import java.util.Map;
import java.util.UUID;

public interface GetLiveSummaryUseCase {

    LiveSummaryResult getSummary(UUID liveId, UUID storeId);

    /**
     * Carrier for the frozen summary plus its read-time (not frozen)
     * batched buyer-name enrichment, resolved with exactly one query
     * regardless of how many orders the summary has.
     */
    record LiveSummaryResult(LiveSummary summary, Map<UUID, String> buyerNames) {}
}
