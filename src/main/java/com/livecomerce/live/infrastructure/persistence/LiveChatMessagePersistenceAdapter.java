package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.application.port.out.LoadChatMessagePort;
import com.livecomerce.live.application.port.out.SaveChatMessagePort;
import com.livecomerce.live.domain.LiveChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class LiveChatMessagePersistenceAdapter implements SaveChatMessagePort, LoadChatMessagePort {

    private final LiveChatMessageJpaRepository repository;

    @Override
    @SuppressWarnings("null")
    public LiveChatMessage save(LiveChatMessage message) {
        return repository.save(message);
    }

    @Override
    public boolean existsByAgoraMsgId(String agoraMsgId) {
        return repository.existsByAgoraMsgId(agoraMsgId);
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
