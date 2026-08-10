package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.UpdateBuyerAddressUseCase;
import com.livecomerce.store.application.port.out.BuyerAddressPort;
import com.livecomerce.store.domain.BuyerAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateBuyerAddressService implements UpdateBuyerAddressUseCase {

    private final BuyerAddressPort buyerAddressPort;

    @Override
    public BuyerAddress update(UpdateBuyerAddressCommand cmd) {
        var address = buyerAddressPort.loadById(cmd.addressId())
                .orElseThrow(() -> new AddressNotFoundException(cmd.addressId()));

        if (!address.getUserId().equals(cmd.userId())) {
            throw new AccessDeniedException("Address does not belong to user");
        }

        address.updateDetails(
                cmd.street(), cmd.extNumber(), cmd.intNumber(), cmd.neighborhood(),
                cmd.city(), cmd.state(), cmd.zipCode(), cmd.country(),
                cmd.latitude(), cmd.longitude(), cmd.addressType()
        );

        return buyerAddressPort.save(address);
    }
}
