package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface LiveJpaRepository extends JpaRepository<Live, UUID> {

    List<Live> findBySellerId(UUID sellerId);

    List<Live> findBySellerIdAndStatus(UUID sellerId, LiveStatus status);

    Optional<Live> findByAgoraChannelId(String agoraChannelId);

    Optional<Live> findByIvsChannelArnAndStatus(String ivsChannelArn, LiveStatus status);

    List<Live> findByStoreId(UUID storeId);

    List<Live> findByStoreIdAndStatus(UUID storeId, LiveStatus status);
}
