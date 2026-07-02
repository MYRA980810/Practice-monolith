package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.application.port.out.LoadLiveSubscriptionPort;
import com.livecomerce.live.application.port.out.SaveLiveSubscriptionPort;
import com.livecomerce.live.domain.LiveSubscription;
import com.livecomerce.live.domain.LiveSubscriptionId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class LiveSubscriptionPersistenceAdapter implements SaveLiveSubscriptionPort, LoadLiveSubscriptionPort {

    private final LiveSubscriptionJpaRepository repository;

    @Override
    @SuppressWarnings("null")
    public void save(LiveSubscription subscription) {
        repository.save(subscription);
    }

    @Override
    public void delete(UUID liveId, UUID buyerId) {
        repository.deleteByLiveIdAndBuyerId(liveId, buyerId);
    }

    @Override
    public void deleteAllByLiveId(UUID liveId) {
        repository.deleteByLiveId(liveId);
    }

    @Override
    @SuppressWarnings("null")
    public boolean existsByLiveIdAndBuyerId(UUID liveId, UUID buyerId) {
        return repository.existsById(new LiveSubscriptionId(liveId, buyerId));
    }

    @Override
    public List<UUID> loadSubscriberIdsByLiveId(UUID liveId) {
        return repository.findBuyerIdsByLiveId(liveId);
    }
}
