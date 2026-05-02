package com.livecomerce.billing.application.port.out;

import com.livecomerce.billing.domain.Subscription;
import com.livecomerce.billing.domain.SubscriptionStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadSubscriptionPort {

    Optional<Subscription> loadByUserId(UUID userId);

    List<Subscription> loadByStatusAndPeriodEndBefore(SubscriptionStatus status, OffsetDateTime threshold);
}
