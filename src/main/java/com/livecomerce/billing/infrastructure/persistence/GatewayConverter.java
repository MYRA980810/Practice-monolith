package com.livecomerce.billing.infrastructure.persistence;

import com.livecomerce.billing.domain.Gateway;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
class GatewayConverter implements AttributeConverter<Gateway, String> {

    @Override
    public String convertToDatabaseColumn(Gateway gateway) {
        return gateway == null ? null : gateway.getCode();
    }

    @Override
    public Gateway convertToEntityAttribute(String code) {
        if (code == null || code.isBlank()) return null;
        return Gateway.fromCode(code);
    }
}
