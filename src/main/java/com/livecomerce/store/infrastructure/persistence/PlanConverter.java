package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.shared.Plan;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
class PlanConverter implements AttributeConverter<Plan, String> {

    @Override
    public String convertToDatabaseColumn(Plan plan) {
        return plan == null ? null : plan.getCode();
    }

    @Override
    public Plan convertToEntityAttribute(String code) {
        if (code == null || code.isBlank()) return null;
        return Plan.fromCode(code);
    }
}
