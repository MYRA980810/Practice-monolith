package com.livecomerce.store.api;

import com.livecomerce.store.domain.BuyerAddress;

import java.util.UUID;

public record BuyerAddressResponse(
        UUID id,
        String street,
        String extNumber,
        String intNumber,
        String neighborhood,
        String city,
        String state,
        String zipCode,
        String country,
        boolean isDefault,
        Double latitude,
        Double longitude
) {
    public static BuyerAddressResponse from(BuyerAddress a) {
        return new BuyerAddressResponse(
                a.getId(),
                a.getStreet(),
                a.getExtNumber(),
                a.getIntNumber(),
                a.getNeighborhood(),
                a.getCity(),
                a.getState(),
                a.getZipCode(),
                a.getCountry(),
                a.isDefault(),
                a.getLatitude(),
                a.getLongitude()
        );
    }
}
