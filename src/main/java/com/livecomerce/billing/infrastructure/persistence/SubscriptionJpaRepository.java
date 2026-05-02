package com.livecomerce.billing.infrastructure.persistence;

import com.livecomerce.billing.domain.Subscription;
import com.livecomerce.billing.domain.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SubscriptionJpaRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByUserId(UUID userId);

    List<Subscription> findByStatusAndCurrentPeriodEndBefore(SubscriptionStatus status, OffsetDateTime threshold);
}
