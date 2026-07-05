package com.livecomerce.analytics.application.port.in;

import com.livecomerce.analytics.domain.LiveSummary;

import java.util.UUID;

public interface GetLiveSummaryUseCase {

    LiveSummary getSummary(UUID liveId, UUID storeId);
}
