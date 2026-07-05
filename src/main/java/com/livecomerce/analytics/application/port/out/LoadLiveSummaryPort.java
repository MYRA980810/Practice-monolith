package com.livecomerce.analytics.application.port.out;

import com.livecomerce.analytics.domain.LiveSummary;

import java.util.Optional;
import java.util.UUID;

public interface LoadLiveSummaryPort {

    Optional<LiveSummary> findByLiveId(UUID liveId);
}
