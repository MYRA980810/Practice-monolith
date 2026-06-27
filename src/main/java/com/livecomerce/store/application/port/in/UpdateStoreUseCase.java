package com.livecomerce.store.application.port.in;

import com.livecomerce.store.domain.Store;
import org.springframework.lang.Nullable;

import java.util.UUID;

public interface UpdateStoreUseCase {

    record ShippingAddressData(
            String street,
            String extNumber,
            String intNumber,
            String neighborhood,
            String city,
            String state,
            String zipCode,
            String country
    ) {}

    record UpdateStoreCommand(
            UUID userId,
            String name,
            String description,
            String logoUrl,
            @Nullable ShippingAddressData shippingAddress
    ) {}

    Store update(UpdateStoreCommand command);
}
