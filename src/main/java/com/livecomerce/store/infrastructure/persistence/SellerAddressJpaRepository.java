package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.store.domain.SellerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SellerAddressJpaRepository extends JpaRepository<SellerAddress, UUID> {

    List<SellerAddress> findByUserId(UUID userId);

    Optional<SellerAddress> findByUserIdAndIsDefaultTrue(UUID userId);

    @Modifying
    @Query("UPDATE SellerAddress s SET s.isDefault = false WHERE s.userId = :userId")
    void clearDefaultByUserId(@Param("userId") UUID userId);

    int countByUserId(UUID userId);
}
