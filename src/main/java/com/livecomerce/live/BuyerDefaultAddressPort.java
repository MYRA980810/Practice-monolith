package com.livecomerce.live;

import java.util.Optional;
import java.util.UUID;

public interface BuyerDefaultAddressPort {

    Optional<ShippingAddress> loadDefaultAddress(UUID userId);

    record ShippingAddress(
            String street,
            String extNumber,
            String intNumber,
            String neighborhood,
            String city,
            String state,
            String zipCode,
            String country
    ) {}
}
