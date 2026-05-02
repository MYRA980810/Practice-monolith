package com.livecomerce.auth.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Role {
    SELLER("SELLER", "Vendedor"),
    BUYER("BUYER",   "Comprador");

    private final String code;
    private final String description;

    Role(String code, String description) {
        this.code        = code;
        this.description = description;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static Role fromCode(String code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(role -> role.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Código de rol inválido: '" + code + "'. " +
                        "Valores aceptados: " + Arrays.toString(values())));
    }

    @Override
    public String toString() {
        return name() + "(" + code + ")";
    }
}
