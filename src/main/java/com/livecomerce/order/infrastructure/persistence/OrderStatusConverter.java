package com.livecomerce.order.infrastructure.persistence;

import com.livecomerce.order.domain.OrderStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {

    @Override
    public String convertToDatabaseColumn(OrderStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public OrderStatus convertToEntityAttribute(String code) {
        if (code == null || code.isBlank()) return null;
        return OrderStatus.fromCode(code);
    }
}
