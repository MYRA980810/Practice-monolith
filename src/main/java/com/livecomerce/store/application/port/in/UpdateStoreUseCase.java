package com.livecomerce.store.application.port.in;

import com.livecomerce.store.domain.Store;

import java.util.UUID;

public interface UpdateStoreUseCase {

    record UpdateStoreCommand(
            UUID userId,
            String name,
            String description,
            String logoUrl
    ) {}

    Store update(UpdateStoreCommand command);
}
