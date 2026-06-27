package com.livecomerce.order.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum OrderItemType {
    PRODUCT("PRODUCT"),
    HOT_PRODUCT("HOT_PRODUCT"),
    SHIPPING("SHIPPING");

    private final String code;

    OrderItemType(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static OrderItemType fromCode(String code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "OrderItemType inválido: '" + code + "'. Valores aceptados: " + Arrays.toString(values())));
    }

    @Override
    public String toString() {
        return name() + "(" + code + ")";
    }
}
