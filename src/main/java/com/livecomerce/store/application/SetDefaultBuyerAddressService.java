package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.SetDefaultBuyerAddressUseCase;
import com.livecomerce.store.application.port.out.BuyerAddressPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SetDefaultBuyerAddressService implements SetDefaultBuyerAddressUseCase {

    private final BuyerAddressPort buyerAddressPort;

    @Override
    public void setDefault(UUID userId, UUID addressId) {
        var address = buyerAddressPort.loadById(addressId)
                .orElseThrow(() -> new AddressNotFoundException(addressId));

        if (!address.getUserId().equals(userId)) {
            throw new AccessDeniedException("Address does not belong to user");
        }

        buyerAddressPort.clearDefaultForUser(userId);
        address.setAsDefault();
        buyerAddressPort.save(address);
    }
}
