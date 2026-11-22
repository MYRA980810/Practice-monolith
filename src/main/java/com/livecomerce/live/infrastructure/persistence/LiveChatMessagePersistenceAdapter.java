package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.application.port.out.LoadChatMessagePort;
import com.livecomerce.live.application.port.out.SaveChatMessagePort;
import com.livecomerce.live.domain.LiveChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class LiveChatMessagePersistenceAdapter implements SaveChatMessagePort, LoadChatMessagePort {

    private final LiveChatMessageJpaRepository repository;

    /**
     * INSERT ... ON CONFLICT DO NOTHING instead of save + catching the constraint violation: the
     * repository's transactional proxy marks the shared transaction rollback-only when the insert
     * throws, so the commit would still fail after the catch.
     */
    @Override
    @Transactional
    public boolean saveIfAbsent(LiveChatMessage message) {
        return repository.insertIfAbsent(message.getId(), message.getLive().getId(), message.getProviderMsgId(),
                message.getUserId(), message.getUsername(), message.getContent(), message.getSentAt()) == 1;
    }

    @Override
    public boolean existsByProviderMsgId(String providerMsgId) {
        return repository.existsByProviderMsgId(providerMsgId);
    }

    @Override
    @Transactional
    public int markDeleted(UUID liveId, String providerMessageId, OffsetDateTime deletedAt) {
        return repository.markDeleted(liveId, providerMessageId, deletedAt);
    }

    @Override
    public List<LiveChatMessage> findByLiveIdBefore(UUID liveId, OffsetDateTime before, int limit) {
        return repository.findByLiveIdAndSentAtBeforeOrderBySentAtDesc(liveId, before, PageRequest.of(0, limit));
    }

    @Override
    public List<LiveChatMessage> findByLiveIdLatest(UUID liveId, int limit) {
        return repository.findByLiveIdOrderBySentAtDesc(liveId, PageRequest.of(0, limit));
    }
}
