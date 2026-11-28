package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.live.SellerShippingAddressPort;
import com.livecomerce.store.application.port.out.SellerAddressPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Seller dispatch address for live orders: the seller's default {@code SellerAddress}.
 * Lives belong to the seller, not to a store, so the deprecated {@code stores.shipping_*}
 * columns are no longer read.
 */
@Component
@RequiredArgsConstructor
class SellerShippingAddressAdapter implements SellerShippingAddressPort {

    private final SellerAddressPort sellerAddressPort;

    @Override
    public Optional<ShippingAddress> loadBySellerId(UUID sellerId) {
        return sellerAddressPort.loadDefaultByUserId(sellerId)
                .map(a -> new ShippingAddress(
                        a.getStreet(), a.getExtNumber(), a.getIntNumber(),
                        a.getNeighborhood(), a.getCity(), a.getState(),
                        a.getZipCode(), a.getCountry()
                ));
    }
}
