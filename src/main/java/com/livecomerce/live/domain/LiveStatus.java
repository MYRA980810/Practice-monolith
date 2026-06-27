package com.livecomerce.live.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum LiveStatus {
    SCHEDULED("SCHEDULED"),
    LIVE("LIVE"),
    ENDED("ENDED"),
    CANCELLED("CANCELLED");

    private final String code;

    LiveStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static LiveStatus fromCode(String code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "LiveStatus invalid: '" + code + "'. Accepted: " + Arrays.toString(values())));
    }

    @Override
    public String toString() {
        return name() + "(" + code + ")";
    }
}
