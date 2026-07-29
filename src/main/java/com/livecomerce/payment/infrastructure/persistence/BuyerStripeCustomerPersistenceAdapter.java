package com.livecomerce.payment.infrastructure.persistence;

import com.livecomerce.payment.application.port.out.BuyerStripeCustomerPort;
import com.livecomerce.payment.domain.BuyerStripeCustomer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class BuyerStripeCustomerPersistenceAdapter implements BuyerStripeCustomerPort {

    private final BuyerStripeCustomerJpaRepository repository;

    @Override
    public Optional<BuyerStripeCustomer> loadByUserId(UUID userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public Optional<BuyerStripeCustomer> loadByStripeCustomerId(String stripeCustomerId) {
        return repository.findByStripeCustomerId(stripeCustomerId);
    }

    @Override
    @SuppressWarnings("null")
    public BuyerStripeCustomer save(BuyerStripeCustomer customer) {
        return repository.save(customer);
    }
}
