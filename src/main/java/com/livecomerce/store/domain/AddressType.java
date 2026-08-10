package com.livecomerce.store.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum AddressType {
    RESIDENTIAL_BUILDING("RESIDENTIAL_BUILDING", "Edificio residencial"),
    STORE("STORE",                               "Tienda"),
    APARTMENT("APARTMENT",                       "Apartamento"),
    HOTEL("HOTEL",                               "Hotel"),
    OFFICE("OFFICE",                             "Oficinas"),
    OTHER("OTHER",                               "Otro");

    private final String code;
    private final String description;

    AddressType(String code, String description) {
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
    public static AddressType fromCode(String code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(t -> t.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de dirección inválido: '" + code + "'. Valores aceptados: " + Arrays.toString(values())));
    }

    @Override
    public String toString() {
        return name() + "(" + code + ")";
    }
}
