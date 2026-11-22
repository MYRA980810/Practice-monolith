package com.livecomerce.live.application.port.out;

import com.livecomerce.live.domain.LiveChatMessage;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface SaveChatMessagePort {

    LiveChatMessage save(LiveChatMessage message);

    boolean existsByAgoraMsgId(String agoraMsgId);

    /**
     * Soft-deletes the message with that provider id, only if it belongs to {@code liveId}.
     *
     * @return the number of rows marked (0 when the message was never persisted or already deleted)
     */
    int markDeleted(UUID liveId, String providerMessageId, OffsetDateTime deletedAt);
}
