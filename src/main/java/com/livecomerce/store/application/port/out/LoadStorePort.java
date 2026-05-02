package com.livecomerce.store.application.port.out;

import com.livecomerce.store.domain.Store;

import java.util.Optional;
import java.util.UUID;

public interface LoadStorePort {

    Optional<Store> loadByUserId(UUID userId);

    Optional<Store> loadBySlug(String slug);
}
