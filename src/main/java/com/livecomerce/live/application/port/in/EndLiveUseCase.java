package com.livecomerce.live.application.port.in;

import com.livecomerce.live.domain.Live;

import java.util.UUID;

public interface EndLiveUseCase {

    Live endLive(EndLiveCommand command);

    record EndLiveCommand(UUID liveId, UUID sellerId) {}
}
