package com.livecomerce.store.application.port.in;

import com.livecomerce.store.domain.Store;

import java.util.UUID;

public interface GetStoreUseCase {

    Store getByUserId(UUID userId);

    Store getBySlug(String slug);

    Store getById(UUID storeId);

    UUID getStoreIdByUserId(UUID userId);
}
