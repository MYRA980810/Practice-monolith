package com.livecomerce.billing.api;

import com.livecomerce.billing.domain.Gateway;
import com.livecomerce.billing.domain.Subscription;
import com.livecomerce.billing.domain.SubscriptionStatus;
import com.livecomerce.shared.Plan;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        UUID userId,
        Plan plan,
        SubscriptionStatus status,
        Gateway gateway,
        OffsetDateTime currentPeriodStart,
        OffsetDateTime currentPeriodEnd
) {
    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getUserId(),
                subscription.getPlan(),
                subscription.getStatus(),
                subscription.getGateway(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd()
        );
    }
}
