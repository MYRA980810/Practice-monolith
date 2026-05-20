package com.livecomerce.store.application.port.in;

import java.util.UUID;

public interface DeactivateStoreUseCase {

    void deactivate(UUID userId);
}
