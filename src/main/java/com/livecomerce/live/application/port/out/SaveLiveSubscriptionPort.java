package com.livecomerce.live.application.port.out;

import com.livecomerce.live.domain.LiveSubscription;

import java.util.UUID;

public interface SaveLiveSubscriptionPort {

    void save(LiveSubscription subscription);

    void delete(UUID liveId, UUID buyerId);

    void deleteAllByLiveId(UUID liveId);
}
