package com.livecomerce.store.application.port.in;

import com.livecomerce.store.domain.SellerAddress;

import java.util.UUID;

public interface AddSellerAddressUseCase {

    record AddSellerAddressCommand(
            UUID userId,
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
    ) {}

    SellerAddress add(AddSellerAddressCommand cmd);
}
