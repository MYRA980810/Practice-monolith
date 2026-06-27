package com.livecomerce.live.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum LiveContext {
    STORE("STORE"),
    SELLER_PROFILE("SELLER_PROFILE");

    private final String code;

    LiveContext(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static LiveContext fromCode(String code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "LiveContext invalid: '" + code + "'. Accepted: " + Arrays.toString(values())));
    }

    @Override
    public String toString() {
        return name() + "(" + code + ")";
    }
}
