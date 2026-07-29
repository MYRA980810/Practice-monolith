package com.livecomerce.payment.infrastructure.persistence;

import com.livecomerce.payment.domain.BuyerStripeCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface BuyerStripeCustomerJpaRepository extends JpaRepository<BuyerStripeCustomer, UUID> {

    Optional<BuyerStripeCustomer> findByUserId(UUID userId);

    Optional<BuyerStripeCustomer> findByStripeCustomerId(String stripeCustomerId);
}
