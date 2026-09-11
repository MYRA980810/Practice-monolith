package com.livecomerce.analytics.application;

import com.livecomerce.analytics.application.port.in.GetStoreRankingUseCase;
import com.livecomerce.analytics.application.port.out.LoadStoreRankingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class GetStoreRankingService implements GetStoreRankingUseCase {

    private final LoadStoreRankingPort loadStoreRankingPort;

    @Override
    public Page<StoreRankingView> getLatestRanking(Pageable pageable) {
        return loadStoreRankingPort.loadLatest(pageable).map(s -> new StoreRankingView(
                s.getStoreId(), s.getRank(), s.getScore(), s.getAvgRating(),
                s.getReviewCount(), s.getFollowerCount(), s.getRecentSalesVolume(), s.getComputedAt()));
    }
}
