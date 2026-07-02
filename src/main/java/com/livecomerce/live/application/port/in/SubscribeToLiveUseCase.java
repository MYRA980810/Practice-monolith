package com.livecomerce.live.application.port.in;

import java.util.UUID;

public interface SubscribeToLiveUseCase {

    void subscribe(SubscribeToLiveCommand command);

    record SubscribeToLiveCommand(UUID liveId, UUID buyerId) {}
}
