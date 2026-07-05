package com.livecomerce.order.application.port.out;

import com.livecomerce.order.domain.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadOrderPort {

    Optional<Order> loadById(UUID orderId);

    Optional<Order> loadActiveByBuyerAndLive(UUID buyerId, UUID liveSessionId);

    List<Order> loadReadyToShipByLive(UUID storeId, UUID liveId);

    List<Order> loadOrdersWithExpiredReservations();
}
