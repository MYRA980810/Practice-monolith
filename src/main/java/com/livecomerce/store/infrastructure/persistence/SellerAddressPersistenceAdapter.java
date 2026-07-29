package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.store.application.port.out.SellerAddressPort;
import com.livecomerce.store.domain.SellerAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class SellerAddressPersistenceAdapter implements SellerAddressPort {

    private final SellerAddressJpaRepository repository;

    @Override
    @SuppressWarnings("null")
    public SellerAddress save(SellerAddress address) {
        return repository.save(address);
    }

    @Override
    @SuppressWarnings("null")
    public Optional<SellerAddress> loadById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<SellerAddress> loadByUserId(UUID userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public Optional<SellerAddress> loadDefaultByUserId(UUID userId) {
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

    @Override
    public int countByUserId(UUID userId) {
        return repository.countByUserId(userId);
    }
}
