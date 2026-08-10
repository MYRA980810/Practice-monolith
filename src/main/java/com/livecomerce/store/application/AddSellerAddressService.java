package com.livecomerce.store.application;

import com.livecomerce.store.SellerAddressAddedEvent;
import com.livecomerce.store.application.port.in.AddSellerAddressUseCase;
import com.livecomerce.store.application.port.out.SellerAddressPort;
import com.livecomerce.store.domain.SellerAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AddSellerAddressService implements AddSellerAddressUseCase {

    private final SellerAddressPort sellerAddressPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public SellerAddress add(AddSellerAddressCommand cmd) {
        if (sellerAddressPort.countByUserId(cmd.userId()) >= 6) {
            throw new AddressLimitExceededException("seller", cmd.userId());
        }

        var address = SellerAddress.create(
                cmd.userId(), cmd.street(), cmd.extNumber(), cmd.intNumber(),
                cmd.neighborhood(), cmd.city(), cmd.state(), cmd.zipCode(), cmd.country(),
                cmd.latitude(), cmd.longitude(),
                cmd.addressType()
        );

        if (cmd.isDefault()) {
            sellerAddressPort.clearDefaultForUser(cmd.userId());
            address.setAsDefault();
        }

        var saved = sellerAddressPort.save(address);
        eventPublisher.publishEvent(new SellerAddressAddedEvent(cmd.userId()));
        return saved;
    }
}
