package com.livecomerce.payment.infrastructure.persistence;

import com.livecomerce.payment.application.port.out.SellerConnectAccountPort;
import com.livecomerce.payment.domain.SellerConnectAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class SellerConnectAccountPersistenceAdapter implements SellerConnectAccountPort {

    private final SellerConnectAccountJpaRepository repository;

    @Override
    public Optional<SellerConnectAccount> loadByUserId(UUID userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public Optional<SellerConnectAccount> loadByStripeAccountId(String stripeAccountId) {
        return repository.findByStripeAccountId(stripeAccountId);
    }

    @Override
    @SuppressWarnings("null")
    public SellerConnectAccount save(SellerConnectAccount account) {
        return repository.save(account);
    }
}
