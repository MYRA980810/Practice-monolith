package com.livecomerce.live.application.port.in;

import com.livecomerce.live.domain.LiveProduct;

import java.util.List;
import java.util.UUID;

public interface ReorderProductsUseCase {

    List<LiveProduct> reorderProducts(ReorderProductsCommand command);

    record ReorderProductsCommand(UUID liveId, UUID sellerId, List<UUID> orderedProductIds) {}
}
