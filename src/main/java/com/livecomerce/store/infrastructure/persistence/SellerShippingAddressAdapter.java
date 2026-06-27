package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.live.SellerShippingAddressPort;
import com.livecomerce.store.application.port.out.LoadStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class SellerShippingAddressAdapter implements SellerShippingAddressPort {

    private final LoadStorePort loadStorePort;

    @Override
    public Optional<ShippingAddress> loadByStoreId(UUID storeId) {
        return loadStorePort.loadById(storeId)
                .filter(s -> s.getShippingStreet() != null)
                .map(s -> new ShippingAddress(
                        s.getShippingStreet(), s.getShippingExtNumber(), s.getShippingIntNumber(),
                        s.getShippingNeighborhood(), s.getShippingCity(), s.getShippingState(),
                        s.getShippingZipCode(), s.getShippingCountry()
                ));
    }
}
