package com.livecomerce.live.application.port.out;

import com.livecomerce.live.domain.LiveChatMessage;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface SaveChatMessagePort {

    /**
     * Inserts the message unless one with the same provider message id already exists. Race-safe:
     * a concurrent duplicate is skipped by the database instead of failing the transaction.
     *
     * @return {@code true} when the message was inserted, {@code false} when it was a duplicate
     */
    boolean saveIfAbsent(LiveChatMessage message);

    boolean existsByProviderMsgId(String providerMsgId);

    /**
     * Soft-deletes the message with that provider id, only if it belongs to {@code liveId}.
     *
     * @return the number of rows marked (0 when the message was never persisted or already deleted)
     */
    int markDeleted(UUID liveId, String providerMessageId, OffsetDateTime deletedAt);
}
