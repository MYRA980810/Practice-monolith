package com.livecomerce.live.application.port.in;

import com.livecomerce.live.domain.LiveProduct;

import java.math.BigDecimal;
import java.util.UUID;

public interface AddHotProductUseCase {

    LiveProduct addHotProduct(AddHotProductCommand command);

    record AddHotProductCommand(
            UUID liveId,
            UUID sellerId,
            String name,
            BigDecimal price,
            String currency,
            int stockAllocated,
            String imageUrl
    ) {}
}
