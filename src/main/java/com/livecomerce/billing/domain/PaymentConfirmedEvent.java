package com.livecomerce.billing.domain;

import com.livecomerce.shared.Plan;

import java.util.UUID;

public record PaymentConfirmedEvent(UUID userId, Plan plan) {}
