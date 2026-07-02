package com.livecomerce.live.application.port.in;

import java.util.UUID;

public interface CheckLiveSubscriptionUseCase {

    boolean isSubscribed(CheckLiveSubscriptionCommand command);

    record CheckLiveSubscriptionCommand(UUID liveId, UUID buyerId) {}
}
