package com.livecomerce.live.application.port.out;

import com.livecomerce.live.domain.LiveChatMessage;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface LoadChatMessagePort {

    List<LiveChatMessage> findByLiveIdBefore(UUID liveId, OffsetDateTime before, int limit);

    List<LiveChatMessage> findByLiveIdLatest(UUID liveId, int limit);
}
