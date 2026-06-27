package com.livecomerce.analytics.application.port.out;

import com.livecomerce.analytics.domain.ChannelComparison;
import com.livecomerce.analytics.domain.LivePerformance;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface LoadChannelMetricsPort {

    ChannelComparison loadChannelComparison(UUID storeId, OffsetDateTime from, OffsetDateTime to);

    List<LivePerformance> loadLivePerformances(UUID storeId, OffsetDateTime from, OffsetDateTime to);
}
