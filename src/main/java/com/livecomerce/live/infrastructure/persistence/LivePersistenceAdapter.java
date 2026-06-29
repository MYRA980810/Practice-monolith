package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
    public List<Live> loadBySellerId(UUID sellerId) {
        return repository.findBySellerId(sellerId);
    }

    @Override
    public List<Live> loadBySellerIdAndStatus(UUID sellerId, LiveStatus status) {
        return repository.findBySellerIdAndStatus(sellerId, status);
    }
}
