package com.livecomerce.analytics.application.port.out;

import com.livecomerce.analytics.domain.StoreRankingSnapshot;

import java.util.List;

public interface SaveStoreRankingPort {

    void replaceAll(List<StoreRankingSnapshot> snapshots);
}
