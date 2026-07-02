package com.livecomerce.live.application.port.out;

import java.util.List;
import java.util.UUID;

public interface LoadLiveSubscriptionPort {

    boolean existsByLiveIdAndBuyerId(UUID liveId, UUID buyerId);

    List<UUID> loadSubscriberIdsByLiveId(UUID liveId);
}
