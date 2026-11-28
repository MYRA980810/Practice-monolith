package com.livecomerce.analytics.application;

import com.livecomerce.analytics.application.port.in.GetLiveSummaryUseCase;
import com.livecomerce.analytics.application.port.out.LoadLiveSummaryPort;
import com.livecomerce.analytics.application.port.out.LoadLiveSummarySourcePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class GetLiveSummaryService implements GetLiveSummaryUseCase {

    private final LoadLiveSummaryPort loadLiveSummaryPort;
    private final LoadLiveSummarySourcePort loadLiveSummarySourcePort;

    @Override
    public LiveSummaryResult getSummary(UUID liveId, UUID sellerId) {
        var summary = loadLiveSummaryPort.findByLiveId(liveId)
                .orElseThrow(() -> new LiveSummaryNotFoundException(liveId));

        if (!Objects.equals(summary.getSellerId(), sellerId)) {
            throw new LiveSummaryNotOwnedException(liveId, sellerId);
        }

        Set<UUID> buyerIds = summary.getOrders().stream()
                .map(order -> order.getBuyerId())
                .collect(java.util.stream.Collectors.toSet());
        var buyerNames = loadLiveSummarySourcePort.findBuyerNames(buyerIds);

        return new LiveSummaryResult(summary, buyerNames);
    }
}
