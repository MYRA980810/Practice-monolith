package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.domain.LiveContext;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LiveContextConverter implements AttributeConverter<LiveContext, String> {

    @Override
    public String convertToDatabaseColumn(LiveContext attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public LiveContext convertToEntityAttribute(String dbData) {
        return dbData == null ? null : LiveContext.fromCode(dbData);
    }
}
