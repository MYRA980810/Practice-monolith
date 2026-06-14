package com.livecomerce.store.application.port.in;

import java.util.UUID;

public interface CloseStoreTemporarilyUseCase {

    void close(UUID userId);
}
