package com.livecomerce.cart.api;

import com.livecomerce.cart.application.port.in.CheckoutCartUseCase.SkippedLine;

import java.util.UUID;

record SkippedLineResponse(UUID productId, UUID variantId, String reason) {

    static SkippedLineResponse from(SkippedLine line) {
        return new SkippedLineResponse(line.productId(), line.variantId(), line.reason());
    }
}
