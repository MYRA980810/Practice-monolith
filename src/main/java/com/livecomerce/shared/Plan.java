package com.livecomerce.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.math.BigDecimal;
import java.util.Arrays;

public enum Plan {
    FREE("FREE",       "Gratis",      new BigDecimal("0.0500"), Money.zero("MXN")),
    STARTER("STARTER", "Inicial",     new BigDecimal("0.0300"), Money.ofMxn("336")),
    PRO("PRO",         "Profesional", new BigDecimal("0.0200"), Money.ofMxn("1050")),
    AGENCY("AGENCY",   "Agencia",     new BigDecimal("0.0100"), Money.ofMxn("2632"));

    private final String code;
    private final String description;
    private final BigDecimal commissionRate;
    private final Money price;

    Plan(String code, String description, BigDecimal commissionRate, Money price) {
        this.code           = code;
        this.description    = description;
        this.commissionRate = commissionRate;
        this.price          = price;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public Money getPrice() {
        return price;
    }

    @JsonCreator
    public static Plan fromCode(String code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(p -> p.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Plan inválido: '" + code + "'. Valores aceptados: " + Arrays.toString(values())));
    }

    @Override
    public String toString() {
        return name() + "(" + code + ")";
    }
}
