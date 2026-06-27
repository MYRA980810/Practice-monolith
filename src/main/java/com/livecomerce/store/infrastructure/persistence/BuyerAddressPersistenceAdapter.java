package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.store.application.port.out.BuyerAddressPort;
import com.livecomerce.store.domain.BuyerAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class BuyerAddressPersistenceAdapter implements BuyerAddressPort {

    private final BuyerAddressJpaRepository repository;

    @Override
    @SuppressWarnings("null")
    public BuyerAddress save(BuyerAddress address) {
        return repository.save(address);
    }

    @Override
    @SuppressWarnings("null")
    public Optional<BuyerAddress> loadById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<BuyerAddress> loadByUserId(UUID userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public Optional<BuyerAddress> loadDefaultByUserId(UUID userId) {
        return repository.findByUserIdAndIsDefaultTrue(userId);
    }

    @Override
    @Transactional
    public void clearDefaultForUser(UUID userId) {
        repository.clearDefaultByUserId(userId);
    }

    @Override
    @SuppressWarnings("null")
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
