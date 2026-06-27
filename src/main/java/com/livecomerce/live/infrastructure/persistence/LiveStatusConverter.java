package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.domain.LiveStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LiveStatusConverter implements AttributeConverter<LiveStatus, String> {

    @Override
    public String convertToDatabaseColumn(LiveStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public LiveStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : LiveStatus.fromCode(dbData);
    }
}
