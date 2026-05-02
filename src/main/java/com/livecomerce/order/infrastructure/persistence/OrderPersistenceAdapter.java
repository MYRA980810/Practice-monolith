package com.livecomerce.order.infrastructure.persistence;

import com.livecomerce.order.application.port.out.LoadOrderPort;
import com.livecomerce.order.application.port.out.SaveOrderPort;
import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderItemStatus;
import com.livecomerce.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class OrderPersistenceAdapter implements LoadOrderPort, SaveOrderPort {

    private final OrderJpaRepository repository;

    @Override
    public Optional<Order> loadById(UUID orderId) {
        return repository.findByIdWithItems(orderId);
    }

    @Override
    public Optional<Order> loadActiveByBuyerAndLive(UUID buyerId, UUID liveSessionId) {
        return repository.findActiveByBuyerAndLive(buyerId, liveSessionId, OrderStatus.OPEN);
    }

    @Override
    public List<Order> loadByStoreId(UUID storeId) {
        return repository.findByStoreId(storeId);
    }

    @Override
    public List<Order> loadOrdersWithExpiredReservations() {
        return repository.findOrdersWithExpiredReservations(OrderItemStatus.RESERVED);
    }

    @Override
    @SuppressWarnings("null")
    public Order save(Order order) {
        return repository.save(order);
    }
}
