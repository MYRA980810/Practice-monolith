package com.livecomerce.payment.api;

import com.livecomerce.payment.domain.BuyerPaymentMethod;

import java.util.UUID;

public record BuyerPaymentMethodResponse(
        UUID id,
        String brand,
        String last4,
        int expMonth,
        int expYear,
        boolean isDefault
) {
    public static BuyerPaymentMethodResponse from(BuyerPaymentMethod paymentMethod) {
        return new BuyerPaymentMethodResponse(
                paymentMethod.getId(),
                paymentMethod.getBrand(),
                paymentMethod.getLast4(),
                paymentMethod.getExpMonth(),
                paymentMethod.getExpYear(),
                paymentMethod.isDefault()
        );
    }
}
