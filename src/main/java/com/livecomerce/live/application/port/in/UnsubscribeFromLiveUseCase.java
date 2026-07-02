package com.livecomerce.live.application.port.in;

import java.util.UUID;

public interface UnsubscribeFromLiveUseCase {

    void unsubscribe(UnsubscribeFromLiveCommand command);

    record UnsubscribeFromLiveCommand(UUID liveId, UUID buyerId) {}
}
