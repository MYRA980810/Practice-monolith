package com.livecomerce.analytics.api;

import com.livecomerce.analytics.application.port.in.GetStoreRankingUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lives in {@code analytics} (not {@code store.api.StoreController}), even
 * though the buyer-facing URL sits under {@code /api/stores} — the ranking is
 * analytics-owned derived data. Keeping {@code store} from importing
 * {@code analytics.application.port.in.GetStoreRankingUseCase} matters here:
 * {@code analytics} already declares {@code store::in} as an allowed
 * dependency, so a reverse {@code store -> analytics} import would close a
 * module cycle (caught by {@code ModularityTests}). Same pattern as
 * {@code LiveController.listByStore} living in {@code live.api} under
 * {@code /api/stores/{storeId}/lives}.
 */
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
class StoreRankingController {

    private final GetStoreRankingUseCase getStoreRankingUseCase;

    @GetMapping("/ranking")
    ResponseEntity<Page<StoreRankingResponse>> getRanking(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(getStoreRankingUseCase.getLatestRanking(pageable).map(StoreRankingResponse::from));
    }
}
