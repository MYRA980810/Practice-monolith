package com.livecomerce.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface StoreReadRepository extends JpaRepository<StoreReadEntity, UUID> {

    Optional<StoreReadEntity> findByUserId(UUID userId);
}
