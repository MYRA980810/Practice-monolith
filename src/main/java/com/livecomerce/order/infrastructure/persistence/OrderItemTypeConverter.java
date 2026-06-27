package com.livecomerce.order.infrastructure.persistence;

import com.livecomerce.order.domain.OrderItemType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
class OrderItemTypeConverter implements AttributeConverter<OrderItemType, String> {

    @Override
    public String convertToDatabaseColumn(OrderItemType type) {
        return type == null ? null : type.getCode();
    }

    @Override
    public OrderItemType convertToEntityAttribute(String code) {
        if (code == null || code.isBlank()) return null;
        return OrderItemType.fromCode(code);
    }
}
