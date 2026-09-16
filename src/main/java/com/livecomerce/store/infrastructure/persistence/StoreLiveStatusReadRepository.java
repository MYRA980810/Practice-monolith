package com.livecomerce.store.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface StoreLiveStatusReadRepository extends JpaRepository<StoreLiveStatusReadEntity, UUID> {

    List<StoreLiveStatusReadEntity> findAllByStoreIdInAndLiveTrue(Collection<UUID> storeIds);
}
