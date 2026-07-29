package com.livecomerce.payment.infrastructure.persistence;

import com.livecomerce.payment.domain.SellerConnectAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SellerConnectAccountJpaRepository extends JpaRepository<SellerConnectAccount, UUID> {

    Optional<SellerConnectAccount> findByUserId(UUID userId);

    Optional<SellerConnectAccount> findByStripeAccountId(String stripeAccountId);
}
