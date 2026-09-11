package com.livecomerce.analytics.application.port.out;

import com.livecomerce.analytics.domain.StoreRankingSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LoadStoreRankingPort {

    Page<StoreRankingSnapshot> loadLatest(Pageable pageable);
}
