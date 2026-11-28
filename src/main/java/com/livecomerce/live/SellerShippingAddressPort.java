package com.livecomerce.live;

import java.util.Optional;
import java.util.UUID;

public interface SellerShippingAddressPort {

    /** The seller's default dispatch address; empty when the seller has none. */
    Optional<ShippingAddress> loadBySellerId(UUID sellerId);

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
