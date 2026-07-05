package com.livecomerce.analytics.application;

import com.livecomerce.analytics.application.port.in.GetLiveSummaryUseCase;
import com.livecomerce.analytics.application.port.out.LoadLiveSummaryPort;
import com.livecomerce.analytics.domain.LiveSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class GetLiveSummaryService implements GetLiveSummaryUseCase {

    private final LoadLiveSummaryPort loadLiveSummaryPort;

    @Override
    public LiveSummary getSummary(UUID liveId, UUID storeId) {
        var summary = loadLiveSummaryPort.findByLiveId(liveId)
                .orElseThrow(() -> new LiveSummaryNotFoundException(liveId));

        if (!Objects.equals(summary.getStoreId(), storeId)) {
            throw new LiveSummaryNotOwnedException(liveId, storeId);
        }

        return summary;
    }
}
