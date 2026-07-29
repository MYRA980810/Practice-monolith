package com.livecomerce.payment.infrastructure.persistence;

import com.livecomerce.payment.application.port.out.BuyerPaymentMethodPort;
import com.livecomerce.payment.domain.BuyerPaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class BuyerPaymentMethodPersistenceAdapter implements BuyerPaymentMethodPort {

    private final BuyerPaymentMethodJpaRepository repository;

    @Override
    @SuppressWarnings("null")
    public BuyerPaymentMethod save(BuyerPaymentMethod paymentMethod) {
        return repository.save(paymentMethod);
    }

    @Override
    @SuppressWarnings("null")
    public Optional<BuyerPaymentMethod> loadById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<BuyerPaymentMethod> loadByUserId(UUID userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public int countByUserId(UUID userId) {
        return repository.countByUserId(userId);
    }

    @Override
    public Optional<BuyerPaymentMethod> loadDefaultByUserId(UUID userId) {
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
