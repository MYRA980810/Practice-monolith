package com.livecomerce.store.api;

import com.livecomerce.store.domain.SellerAddress;

import java.util.UUID;

public record SellerAddressResponse(
        UUID id,
        String street,
        String extNumber,
        String intNumber,
        String neighborhood,
        String city,
        String state,
        String zipCode,
        String country,
        boolean isDefault
) {
    public static SellerAddressResponse from(SellerAddress a) {
        return new SellerAddressResponse(
                a.getId(),
                a.getStreet(),
                a.getExtNumber(),
                a.getIntNumber(),
                a.getNeighborhood(),
                a.getCity(),
                a.getState(),
                a.getZipCode(),
                a.getCountry(),
                a.isDefault()
        );
    }
}
