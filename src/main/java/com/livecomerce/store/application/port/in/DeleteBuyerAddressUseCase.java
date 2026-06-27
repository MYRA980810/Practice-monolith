package com.livecomerce.store.application.port.in;

import java.util.UUID;

public interface DeleteBuyerAddressUseCase {

    void delete(UUID userId, UUID addressId);
}
