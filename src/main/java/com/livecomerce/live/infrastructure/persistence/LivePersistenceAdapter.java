package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class LivePersistenceAdapter implements SaveLivePort, LoadLivePort {

    private final LiveJpaRepository repository;

    @Override
    @SuppressWarnings("null")
    public Live save(Live live) {
        return repository.save(live);
    }

    @Override
    @SuppressWarnings("null") // UUID is non-null by contract; JPA findById expects @NonNull
    public Optional<Live> loadById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Live> loadByAgoraChannelId(String agoraChannelId) {
        return repository.findByAgoraChannelId(agoraChannelId);
    }

    @Override
    public Optional<Live> loadActiveByIvsChannelArn(String ivsChannelArn) {
        return repository.findByIvsChannelArnAndStatus(ivsChannelArn, LiveStatus.LIVE);
    }

    @Override
    public List<Live> loadBySellerId(UUID sellerId) {
        return repository.findBySellerId(sellerId);
    }

    @Override
    public List<Live> loadBySellerIdAndStatus(UUID sellerId, LiveStatus status) {
        return repository.findBySellerIdAndStatus(sellerId, status);
    }

    @Override
    public List<Live> loadByStoreId(UUID storeId) {
        return repository.findByStoreId(storeId);
    }

    @Override
    public List<Live> loadByStoreIdAndStatus(UUID storeId, LiveStatus status) {
        return repository.findByStoreIdAndStatus(storeId, status);
    }

    @Override
    public Page<Live> loadByStatus(LiveStatus status, Pageable pageable) {
        return repository.findByStatus(status, pageable);
    }

    @Override
    public Page<Live> loadUpcoming(Pageable pageable) {
        return repository.findByStatusAndScheduledAtIsNotNull(LiveStatus.SCHEDULED, pageable);
    }

    @Override
    public List<Live> loadStaleLive(Instant cutoff) {
        return repository.findByStatusAndStreamEndedAtLessThanEqual(LiveStatus.LIVE, cutoff);
    }
}
