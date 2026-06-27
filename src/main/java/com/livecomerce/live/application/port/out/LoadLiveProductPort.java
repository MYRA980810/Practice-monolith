package com.livecomerce.live.application.port.out;

import com.livecomerce.live.domain.LiveProduct;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadLiveProductPort {

    Optional<LiveProduct> loadById(UUID id);

    List<LiveProduct> loadByLiveId(UUID liveId);

    Optional<LiveProduct> loadPinnedByLiveId(UUID liveId);
}
