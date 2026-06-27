package com.livecomerce.live.application.port.in;

import com.livecomerce.live.domain.LiveProduct;

import java.util.UUID;

public interface UnpinProductUseCase {

    LiveProduct unpinProduct(UnpinProductCommand command);

    record UnpinProductCommand(UUID liveId, UUID liveProductId, UUID sellerId) {}
}
