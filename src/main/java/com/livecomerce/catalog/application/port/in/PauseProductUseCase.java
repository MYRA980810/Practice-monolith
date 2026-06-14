package com.livecomerce.catalog.application.port.in;

import java.util.UUID;

public interface PauseProductUseCase {

    record PauseCommand(UUID productId, UUID storeId) {}

    void pause(PauseCommand command);
}
