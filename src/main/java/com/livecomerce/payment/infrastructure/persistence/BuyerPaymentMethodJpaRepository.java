package com.livecomerce.payment.infrastructure.persistence;

import com.livecomerce.payment.domain.BuyerPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface BuyerPaymentMethodJpaRepository extends JpaRepository<BuyerPaymentMethod, UUID> {

    List<BuyerPaymentMethod> findByUserId(UUID userId);

    Optional<BuyerPaymentMethod> findByUserIdAndIsDefaultTrue(UUID userId);

    int countByUserId(UUID userId);

    @Modifying
    @Query("UPDATE BuyerPaymentMethod b SET b.isDefault = false WHERE b.userId = :userId")
    void clearDefaultByUserId(@Param("userId") UUID userId);
}
