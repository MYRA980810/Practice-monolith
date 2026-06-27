package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.store.domain.BuyerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface BuyerAddressJpaRepository extends JpaRepository<BuyerAddress, UUID> {

    List<BuyerAddress> findByUserId(UUID userId);

    Optional<BuyerAddress> findByUserIdAndIsDefaultTrue(UUID userId);

    @Modifying
    @Query("UPDATE BuyerAddress b SET b.isDefault = false WHERE b.userId = :userId")
    void clearDefaultByUserId(@Param("userId") UUID userId);
}
