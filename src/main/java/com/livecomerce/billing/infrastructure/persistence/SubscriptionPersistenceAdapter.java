package com.livecomerce.billing.infrastructure.persistence;

import com.livecomerce.billing.application.port.out.LoadSubscriptionPort;
import com.livecomerce.billing.application.port.out.SaveSubscriptionPort;
import com.livecomerce.billing.domain.Subscription;
import com.livecomerce.billing.domain.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class SubscriptionPersistenceAdapter implements LoadSubscriptionPort, SaveSubscriptionPort {

    private final SubscriptionJpaRepository repository;

    @Override
    public Optional<Subscription> loadByUserId(UUID userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public List<Subscription> loadByStatusAndPeriodEndBefore(SubscriptionStatus status, OffsetDateTime threshold) {
        return repository.findByStatusAndCurrentPeriodEndBefore(status, threshold);
    }

    @Override
    @SuppressWarnings("null")
    public Subscription save(Subscription subscription) {
        return repository.save(subscription);
    }
}
