package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.application.port.out.AtomicLiveProductStockPort;
import com.livecomerce.live.application.port.out.LoadLiveProductPort;
import com.livecomerce.live.application.port.out.SaveLiveProductPort;
import com.livecomerce.live.domain.LiveProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class LiveProductPersistenceAdapter
        implements SaveLiveProductPort, LoadLiveProductPort, AtomicLiveProductStockPort {

    private final LiveProductJpaRepository repository;

    @Override
    @SuppressWarnings("null")
    public LiveProduct save(LiveProduct product) {
        return repository.save(product);
    }

    @Override
    @SuppressWarnings("null")
    public List<LiveProduct> saveAll(List<LiveProduct> products) {
        return repository.saveAll(products);
    }

    @Override
    @SuppressWarnings("null") // UUID is non-null by contract; JPA findById expects @NonNull
    public Optional<LiveProduct> loadById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<LiveProduct> loadByLiveId(UUID liveId) {
        return repository.findByLiveId(liveId);
    }

    @Override
    public Optional<LiveProduct> loadPinnedByLiveId(UUID liveId) {
        return repository.findPinnedByLiveId(liveId);
    }

    @Override
    public Optional<UUID> atomicIncrementStockSold(UUID liveProductId, int qty) {
        return repository.atomicIncrementStockSold(liveProductId, qty);
    }

    @Override
    public void decrementStockSold(UUID liveProductId, int qty) {
        repository.decrementStockSold(liveProductId, qty);
    }
}
