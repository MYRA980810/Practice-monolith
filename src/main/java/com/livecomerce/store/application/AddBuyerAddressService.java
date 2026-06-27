package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.AddBuyerAddressUseCase;
import com.livecomerce.store.application.port.out.BuyerAddressPort;
import com.livecomerce.store.domain.BuyerAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AddBuyerAddressService implements AddBuyerAddressUseCase {

    private final BuyerAddressPort buyerAddressPort;

    @Override
    public BuyerAddress add(AddBuyerAddressCommand cmd) {
        var address = BuyerAddress.create(
                cmd.userId(), cmd.street(), cmd.extNumber(), cmd.intNumber(),
                cmd.neighborhood(), cmd.city(), cmd.state(), cmd.zipCode(), cmd.country()
        );

        if (cmd.isDefault()) {
            buyerAddressPort.clearDefaultForUser(cmd.userId());
            address.setAsDefault();
        }

        return buyerAddressPort.save(address);
    }
}
