package com.livecomerce.billing.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Gateway {
    MERCADOPAGO("MERCADOPAGO"),
    CONEKTA("CONEKTA"),
    STRIPE("STRIPE");

    private final String code;

    Gateway(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static Gateway fromCode(String code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(g -> g.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Gateway inválido: '" + code + "'. Valores aceptados: " + Arrays.toString(values())));
    }

    @Override
    public String toString() {
        return name() + "(" + code + ")";
    }
}
