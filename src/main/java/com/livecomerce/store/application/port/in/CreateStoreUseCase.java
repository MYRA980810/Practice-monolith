package com.livecomerce.store.application.port.in;

import com.livecomerce.store.domain.Store;

import java.util.UUID;

public interface CreateStoreUseCase {

    record CreateStoreCommand(
            UUID userId,
            String name,
            String slug,
            String description
    ) {}

    Store create(CreateStoreCommand command);
}
