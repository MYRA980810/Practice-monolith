package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.SetDefaultSellerAddressUseCase;
import com.livecomerce.store.application.port.out.SellerAddressPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SetDefaultSellerAddressService implements SetDefaultSellerAddressUseCase {

    private final SellerAddressPort sellerAddressPort;

    @Override
    public void setDefault(UUID userId, UUID addressId) {
        var address = sellerAddressPort.loadById(addressId)
                .orElseThrow(() -> new AddressNotFoundException(addressId));

        if (!address.getUserId().equals(userId)) {
            throw new AccessDeniedException("Address does not belong to user");
        }

        sellerAddressPort.clearDefaultForUser(userId);
        address.setAsDefault();
        sellerAddressPort.save(address);
    }
}
