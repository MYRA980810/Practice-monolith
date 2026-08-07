package com.livecomerce.store.application.port.in;

import com.livecomerce.store.domain.BuyerAddress;

import java.util.UUID;

public interface AddBuyerAddressUseCase {

    record AddBuyerAddressCommand(
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

    BuyerAddress add(AddBuyerAddressCommand cmd);
}
