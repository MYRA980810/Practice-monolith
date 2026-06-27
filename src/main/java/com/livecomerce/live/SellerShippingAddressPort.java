package com.livecomerce.live;

import java.util.Optional;
import java.util.UUID;

public interface SellerShippingAddressPort {

    Optional<ShippingAddress> loadByStoreId(UUID storeId);

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
