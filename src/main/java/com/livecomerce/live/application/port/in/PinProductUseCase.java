package com.livecomerce.live.application.port.in;

import com.livecomerce.live.domain.LiveProduct;

import java.util.UUID;

public interface PinProductUseCase {

    LiveProduct pinProduct(PinProductCommand command);

    record PinProductCommand(UUID liveId, UUID liveProductId, UUID sellerId) {}
}
