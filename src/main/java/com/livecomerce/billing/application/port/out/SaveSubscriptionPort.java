package com.livecomerce.billing.application.port.out;

import com.livecomerce.billing.domain.Subscription;

public interface SaveSubscriptionPort {
    Subscription save(Subscription subscription);
}
