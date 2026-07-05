package com.livecomerce.order.api;

import com.livecomerce.order.domain.OrderItem;
import com.livecomerce.order.domain.OrderItemStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        String productName,
        BigDecimal unitPrice,
        String currency,
        int quantity,
        BigDecimal subtotal,
        OrderItemStatus status,
        OffsetDateTime reservedUntil,
        OffsetDateTime paidAt,
        String imageUrl
) {
    public static OrderItemResponse from(OrderItem item) {
        return from(item, null);
    }

    public static OrderItemResponse from(OrderItem item, String imageUrl) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getCurrency(),
                item.getQuantity(),
                item.getSubtotal(),
                item.getStatus(),
                item.getReservedUntil(),
                item.getPaidAt(),
                imageUrl
        );
    }
}
