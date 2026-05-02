package com.livecomerce.order.application;

import com.livecomerce.order.application.port.in.GetOrderUseCase;
import com.livecomerce.order.application.port.out.LoadOrderPort;
import com.livecomerce.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetOrderService implements GetOrderUseCase {

    private final LoadOrderPort loadOrderPort;

    @Override
    public Order getById(UUID orderId) {
        return loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Override
    public Optional<Order> getActiveOrder(UUID buyerId, UUID liveSessionId) {
        return loadOrderPort.loadActiveByBuyerAndLive(buyerId, liveSessionId);
    }

    @Override
    public List<Order> getByStore(UUID storeId) {
        return loadOrderPort.loadByStoreId(storeId);
    }
}
