package com.livecomerce.billing.application.port.in;

import com.livecomerce.billing.domain.Subscription;

import java.util.UUID;

public interface GetSubscriptionUseCase {

    Subscription getByUserId(UUID userId);
}
