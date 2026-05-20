package com.livecomerce.store.application.port.in;

import java.util.UUID;

public interface ReactivateStoreUseCase {

    void reactivate(UUID userId);
}
