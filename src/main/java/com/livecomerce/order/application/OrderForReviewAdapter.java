package com.livecomerce.order.application;

import com.livecomerce.order.LoadOrderForReviewPort;
import com.livecomerce.order.application.port.out.LoadOrderPort;
import com.livecomerce.order.domain.OrderItemType;
import com.livecomerce.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class OrderForReviewAdapter implements LoadOrderForReviewPort {

    private final LoadOrderPort loadOrderPort;

    @Override
    public Optional<OrderForReview> loadForReview(UUID orderId) {
        return loadOrderPort.loadById(orderId).map(order -> {
            var items = order.getItems().stream()
                    .filter(item -> item.getItemType() == OrderItemType.PRODUCT)
                    .map(item -> new OrderLineForReview(item.getId(), item.getProductId()))
                    .toList();
            return new OrderForReview(
                    order.getId(),
                    order.getBuyerId(),
                    order.getStoreId(),
                    order.getStatus() == OrderStatus.DELIVERED,
                    items);
        });
    }
}
