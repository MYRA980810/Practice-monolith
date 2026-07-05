package com.livecomerce.order.application;

import com.livecomerce.order.application.port.in.GetOrderUseCase;
import com.livecomerce.order.application.port.out.LoadLiveProductImagePort;
import com.livecomerce.order.application.port.out.LoadOrderPort;
import com.livecomerce.order.application.port.out.LoadStoreIdPort;
import com.livecomerce.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetOrderService implements GetOrderUseCase {

    private final LoadOrderPort loadOrderPort;
    private final LoadLiveProductImagePort loadLiveProductImagePort;
    private final LoadStoreIdPort loadStoreIdPort;

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
    public ReadyToShipResult getReadyToShipByLive(UUID sellerId, UUID liveId) {
        var storeId = loadStoreIdPort.findStoreIdByUserId(sellerId)
                .orElseThrow(() -> new StoreNotFoundException(sellerId));
        var orders = loadOrderPort.loadReadyToShipByLive(storeId, liveId);
        if (orders.isEmpty()) {
            return new ReadyToShipResult(orders, Map.of());
        }

        Set<UUID> productIds = orders.stream()
                .flatMap(order -> order.getItems().stream())
                .map(item -> item.getProductId())
                .collect(java.util.stream.Collectors.toSet());

        var imageUrlByProductId = loadLiveProductImagePort.findImageUrls(liveId, productIds);
        return new ReadyToShipResult(orders, imageUrlByProductId);
    }
}
