package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.store.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface StoreJpaRepository extends JpaRepository<Store, UUID> {

    Optional<Store> findByUserId(UUID userId);

    Optional<Store> findBySlug(String slug);
}
