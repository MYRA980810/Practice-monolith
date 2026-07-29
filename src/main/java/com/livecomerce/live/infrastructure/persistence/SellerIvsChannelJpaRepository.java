package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.domain.SellerIvsChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SellerIvsChannelJpaRepository extends JpaRepository<SellerIvsChannel, UUID> {

    Optional<SellerIvsChannel> findBySellerId(UUID sellerId);
}
