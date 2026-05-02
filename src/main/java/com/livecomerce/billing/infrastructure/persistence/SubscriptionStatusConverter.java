package com.livecomerce.billing.infrastructure.persistence;

import com.livecomerce.billing.domain.SubscriptionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
class SubscriptionStatusConverter implements AttributeConverter<SubscriptionStatus, String> {

    @Override
    public String convertToDatabaseColumn(SubscriptionStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public SubscriptionStatus convertToEntityAttribute(String code) {
        if (code == null || code.isBlank()) return null;
        return SubscriptionStatus.fromCode(code);
    }
}
