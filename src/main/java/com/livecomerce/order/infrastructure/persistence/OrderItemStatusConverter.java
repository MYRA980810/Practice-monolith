package com.livecomerce.order.infrastructure.persistence;

import com.livecomerce.order.domain.OrderItemStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
class OrderItemStatusConverter implements AttributeConverter<OrderItemStatus, String> {

    @Override
    public String convertToDatabaseColumn(OrderItemStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public OrderItemStatus convertToEntityAttribute(String code) {
        if (code == null || code.isBlank()) return null;
        return OrderItemStatus.fromCode(code);
    }
}
