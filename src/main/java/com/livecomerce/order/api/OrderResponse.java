package com.livecomerce.order.api;

import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID buyerId,
        UUID storeId,
        UUID liveSessionId,
        OrderStatus status,
        String currency,
        String shippingAddress,
        String trackingNumber,
        List<OrderItemResponse> items,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getBuyerId(),
                order.getStoreId(),
                order.getLiveSessionId(),
                order.getStatus(),
                order.getCurrency(),
                order.getShippingAddress(),
                order.getTrackingNumber(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
