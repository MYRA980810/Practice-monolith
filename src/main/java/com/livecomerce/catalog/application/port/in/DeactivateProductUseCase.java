package com.livecomerce.catalog.application.port.in;

import java.util.UUID;

public interface DeactivateProductUseCase {

    record DeactivateCommand(UUID productId, UUID storeId) {}

    void deactivate(DeactivateCommand command);
}
