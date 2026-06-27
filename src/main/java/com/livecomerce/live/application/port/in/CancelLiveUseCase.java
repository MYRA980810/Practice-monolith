package com.livecomerce.live.application.port.in;

import com.livecomerce.live.domain.Live;

import java.util.UUID;

public interface CancelLiveUseCase {

    Live cancelLive(CancelLiveCommand command);

    record CancelLiveCommand(UUID liveId, UUID sellerId) {}
}
