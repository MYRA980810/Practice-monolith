package com.livecomerce.store.application.port.out;

import com.livecomerce.store.domain.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadStorePort {

    Optional<Store> loadByUserId(UUID userId);

    Optional<Store> loadBySlug(String slug);

    Optional<Store> loadById(UUID storeId);

    List<Store> loadByIds(Collection<UUID> storeIds);

    Page<Store> loadAllActive(Pageable pageable);
}
