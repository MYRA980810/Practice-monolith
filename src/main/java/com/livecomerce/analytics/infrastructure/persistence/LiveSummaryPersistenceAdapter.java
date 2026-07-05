package com.livecomerce.analytics.infrastructure.persistence;

import com.livecomerce.analytics.application.port.out.LoadLiveSummaryPort;
import com.livecomerce.analytics.application.port.out.LoadLiveSummarySourcePort;
import com.livecomerce.analytics.application.port.out.SaveLiveSummaryPort;
import com.livecomerce.analytics.domain.LiveSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class LiveSummaryPersistenceAdapter implements SaveLiveSummaryPort, LoadLiveSummaryPort, LoadLiveSummarySourcePort {

    private final LiveSummaryRepository liveSummaryRepository;
    private final LiveSummaryReadRepository liveSummaryReadRepository;
    private final LiveSummaryOrderDetailRepository liveSummaryOrderDetailRepository;
    private final LiveProductReadRepository liveProductReadRepository;
    private final BuyerNameRepository buyerNameRepository;

    @Override
    public LiveSummary save(LiveSummary summary) {
        return liveSummaryRepository.save(summary);
    }

    @Override
    public boolean existsByLiveId(UUID liveId) {
        return liveSummaryRepository.existsById(liveId);
    }

    @Override
    public Optional<LiveSummary> findByLiveId(UUID liveId) {
        return liveSummaryRepository.findById(liveId);
    }

    @Override
    public Optional<LiveSnapshot> findLiveSnapshot(UUID liveId) {
        return liveSummaryReadRepository.findById(liveId)
                .map(entity -> new LiveSnapshot(
                        entity.getStoreId(),
                        entity.getStartedAt(),
                        entity.getEndedAt(),
                        entity.getPeakViewers()));
    }

    @Override
    public List<QualifyingOrder> findQualifyingOrders(UUID liveId) {
        return liveSummaryOrderDetailRepository.findQualifyingOrderDetails(liveId).stream()
                .map(row -> new QualifyingOrder(
                        (UUID) row[0],
                        (UUID) row[1],
                        (String) row[3],
                        toBigDecimal(row[2]),
                        ((Number) row[4]).intValue()))
                .toList();
    }

    @Override
    public long findTotalAllocated(UUID liveId) {
        return liveProductReadRepository.sumStockAllocatedByLiveId(liveId);
    }

    @Override
    public Map<UUID, String> findBuyerNames(Collection<UUID> buyerIds) {
        if (buyerIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> names = new HashMap<>();
        for (Object[] row : buyerNameRepository.findNamesByIds(buyerIds)) {
            var buyerId = (UUID) row[0];
            var fullName = row[1] + " " + row[2];
            names.put(buyerId, fullName);
        }
        return names;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }
}
