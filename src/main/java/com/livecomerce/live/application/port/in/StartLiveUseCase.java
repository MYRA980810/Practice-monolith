package com.livecomerce.live.application.port.in;

import com.livecomerce.live.domain.Live;

import java.util.UUID;

public interface StartLiveUseCase {

    Live startLive(StartLiveCommand command);

    record StartLiveCommand(UUID liveId, UUID sellerId, String rtcUid) {}
}
