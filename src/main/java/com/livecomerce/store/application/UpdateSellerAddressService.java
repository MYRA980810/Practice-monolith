package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.UpdateSellerAddressUseCase;
import com.livecomerce.store.application.port.out.SellerAddressPort;
import com.livecomerce.store.domain.SellerAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateSellerAddressService implements UpdateSellerAddressUseCase {

    private final SellerAddressPort sellerAddressPort;

    @Override
    public SellerAddress update(UpdateSellerAddressCommand cmd) {
        var address = sellerAddressPort.loadById(cmd.addressId())
                .orElseThrow(() -> new AddressNotFoundException(cmd.addressId()));

        if (!address.getUserId().equals(cmd.userId())) {
            throw new AccessDeniedException("Address does not belong to user");
        }

        address.updateDetails(
                cmd.street(), cmd.extNumber(), cmd.intNumber(), cmd.neighborhood(),
                cmd.city(), cmd.state(), cmd.zipCode(), cmd.country(),
                cmd.latitude(), cmd.longitude(), cmd.addressType()
        );

        return sellerAddressPort.save(address);
    }
}
