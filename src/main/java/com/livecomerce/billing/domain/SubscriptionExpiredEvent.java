package com.livecomerce.billing.domain;

import java.util.UUID;

public record SubscriptionExpiredEvent(UUID userId) {}
