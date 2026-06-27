package com.livecomerce.store.application.port.in;

import java.util.UUID;

public interface SetDefaultBuyerAddressUseCase {

    void setDefault(UUID userId, UUID addressId);
}
