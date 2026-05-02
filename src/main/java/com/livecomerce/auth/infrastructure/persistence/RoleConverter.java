package com.livecomerce.auth.infrastructure.persistence;

import com.livecomerce.auth.domain.Role;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
class RoleConverter implements AttributeConverter<Role, String> {

    @Override
    public String convertToDatabaseColumn(Role role) {
        return role == null ? null : role.getCode();
    }

    @Override
    public Role convertToEntityAttribute(String code) {
        if (code == null || code.isBlank()) return null;
        return Role.fromCode(code);
    }
}
