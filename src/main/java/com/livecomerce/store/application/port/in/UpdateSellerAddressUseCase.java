package com.livecomerce.store.application.port.in;

import com.livecomerce.store.domain.AddressType;
import com.livecomerce.store.domain.SellerAddress;

import java.util.UUID;

public interface UpdateSellerAddressUseCase {

    record UpdateSellerAddressCommand(
            UUID userId,
            UUID addressId,
            String street,
            String extNumber,
            String intNumber,
            String neighborhood,
            String city,
            String state,
            String zipCode,
            String country,
            Double latitude,
            Double longitude,
            AddressType addressType
    ) {}

    SellerAddress update(UpdateSellerAddressCommand cmd);
}
