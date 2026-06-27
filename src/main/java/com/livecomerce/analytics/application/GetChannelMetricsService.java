package com.livecomerce.analytics.application;

import com.livecomerce.analytics.application.port.in.GetChannelMetricsUseCase;
import com.livecomerce.analytics.application.port.out.LoadChannelMetricsPort;
import com.livecomerce.analytics.domain.ChannelComparison;
import com.livecomerce.analytics.domain.LivePerformance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class GetChannelMetricsService implements GetChannelMetricsUseCase {

    private final LoadChannelMetricsPort loadChannelMetricsPort;

    @Override
    public ChannelComparison getChannelComparison(UUID storeId, OffsetDateTime from, OffsetDateTime to) {
        return loadChannelMetricsPort.loadChannelComparison(storeId, from, to);
    }

    @Override
    public List<LivePerformance> getLivePerformances(UUID storeId, OffsetDateTime from, OffsetDateTime to) {
        return loadChannelMetricsPort.loadLivePerformances(storeId, from, to);
    }
}
