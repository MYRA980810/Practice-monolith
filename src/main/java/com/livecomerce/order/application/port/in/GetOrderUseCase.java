package com.livecomerce.order.application.port.in;

import com.livecomerce.order.domain.Order;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface GetOrderUseCase {

    Order getById(UUID orderId);

    Optional<Order> getActiveOrder(UUID buyerId, UUID liveSessionId);

    /**
     * Ready-to-ship orders (PAID) for one live, scoped to the seller's own
     * store. {@code sellerId} is the authenticated user id (JWT), never a
     * client-supplied storeId — the storeId is resolved server-side from it.
     * A foreign/unowned {@code liveId} yields an empty result — no
     * exception, no 403/404 (deliberate IDOR-safe design: the composite
     * store+live filter simply matches zero rows).
     */
    ReadyToShipResult getReadyToShipByLive(UUID sellerId, UUID liveId);

    record ReadyToShipResult(List<Order> orders, Map<UUID, String> imageUrlByProductId) {}
}
