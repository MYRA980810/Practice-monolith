package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.store.domain.StoreFollower;
import com.livecomerce.store.domain.StoreFollowerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface StoreFollowerJpaRepository extends JpaRepository<StoreFollower, StoreFollowerId> {

    long countByStoreId(UUID storeId);

    boolean existsByStoreIdAndUserId(UUID storeId, UUID userId);

    @Modifying
    void deleteByStoreIdAndUserId(UUID storeId, UUID userId);

    @Query("SELECT sf.storeId FROM StoreFollower sf WHERE sf.userId = :userId")
    List<UUID> findStoreIdsByUserId(@Param("userId") UUID userId);
}
