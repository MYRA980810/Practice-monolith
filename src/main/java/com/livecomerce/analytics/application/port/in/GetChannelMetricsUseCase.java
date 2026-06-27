package com.livecomerce.analytics.application.port.in;

import com.livecomerce.analytics.domain.ChannelComparison;
import com.livecomerce.analytics.domain.LivePerformance;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface GetChannelMetricsUseCase {

    ChannelComparison getChannelComparison(UUID storeId, OffsetDateTime from, OffsetDateTime to);

    List<LivePerformance> getLivePerformances(UUID storeId, OffsetDateTime from, OffsetDateTime to);
}
