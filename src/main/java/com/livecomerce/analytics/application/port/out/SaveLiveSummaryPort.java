package com.livecomerce.analytics.application.port.out;

import com.livecomerce.analytics.domain.LiveSummary;

import java.util.UUID;

public interface SaveLiveSummaryPort {

    LiveSummary save(LiveSummary summary);

    boolean existsByLiveId(UUID liveId);
}
