package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.live.BuyerDefaultAddressPort;
import com.livecomerce.store.application.port.out.BuyerAddressPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class BuyerDefaultAddressAdapter implements BuyerDefaultAddressPort {

    private final BuyerAddressPort buyerAddressPort;

    @Override
    public Optional<ShippingAddress> loadDefaultAddress(UUID userId) {
        return buyerAddressPort.loadDefaultByUserId(userId)
                .map(a -> new ShippingAddress(
                        a.getStreet(), a.getExtNumber(), a.getIntNumber(),
                        a.getNeighborhood(), a.getCity(), a.getState(),
                        a.getZipCode(), a.getCountry()
                ));
    }
}
