package com.livecomerce.store.application.port.in;

import java.util.UUID;

public interface SetDefaultSellerAddressUseCase {

    void setDefault(UUID userId, UUID addressId);
}
