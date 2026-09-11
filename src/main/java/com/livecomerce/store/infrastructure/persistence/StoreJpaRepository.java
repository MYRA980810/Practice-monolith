package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.store.domain.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface StoreJpaRepository extends JpaRepository<Store, UUID> {

    Optional<Store> findByUserId(UUID userId);

    Optional<Store> findBySlug(String slug);

    Page<Store> findAllByActiveTrueAndTemporarilyClosedFalse(Pageable pageable);

    @Query("SELECT s.id FROM Store s WHERE s.active = true AND s.temporarilyClosed = false")
    List<UUID> findActiveIds();
}
