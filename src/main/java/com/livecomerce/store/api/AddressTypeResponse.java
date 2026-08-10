package com.livecomerce.store.api;

import com.livecomerce.store.domain.AddressType;

public record AddressTypeResponse(
        String code,
        String description
) {
    public static AddressTypeResponse from(AddressType type) {
        return new AddressTypeResponse(
                type.getCode(),
                type.getDescription()
        );
    }
}
