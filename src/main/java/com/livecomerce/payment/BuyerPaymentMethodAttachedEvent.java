package com.livecomerce.payment;

import java.util.UUID;

public record BuyerPaymentMethodAttachedEvent(UUID userId, UUID buyerPaymentMethodId) {}
