package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.DeleteBuyerAddressUseCase;
import com.livecomerce.store.application.port.out.BuyerAddressPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteBuyerAddressService implements DeleteBuyerAddressUseCase {

    private final BuyerAddressPort buyerAddressPort;

    @Override
    public void delete(UUID userId, UUID addressId) {
        var address = buyerAddressPort.loadById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));

        if (!address.getUserId().equals(userId)) {
            throw new AccessDeniedException("Address does not belong to user");
        }

        buyerAddressPort.deleteById(addressId);
    }
}
