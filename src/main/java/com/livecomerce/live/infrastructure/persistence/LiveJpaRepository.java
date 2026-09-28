package com.livecomerce.live.infrastructure.persistence;

import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface LiveJpaRepository extends JpaRepository<Live, UUID> {

    List<Live> findBySellerId(UUID sellerId);

    List<Live> findBySellerIdAndStatus(UUID sellerId, LiveStatus status);

    Optional<Live> findByAgoraChannelId(String agoraChannelId);

    Optional<Live> findByIvsChannelArnAndStatusIn(String ivsChannelArn, Collection<LiveStatus> statuses);

    List<Live> findByStoreId(UUID storeId);

    List<Live> findByStoreIdAndStatus(UUID storeId, LiveStatus status);

    Page<Live> findByStatus(LiveStatus status, Pageable pageable);

    Page<Live> findByStatusAndScheduledAtIsNotNull(LiveStatus status, Pageable pageable);

    Page<Live> findByStatusAndCategoryId(LiveStatus status, UUID categoryId, Pageable pageable);

    Page<Live> findByStatusAndCategoryIdAndScheduledAtIsNotNull(LiveStatus status, UUID categoryId, Pageable pageable);

    List<Live> findByStatusAndStreamEndedAtLessThanEqual(LiveStatus status, Instant cutoff);

    /** Rows of {@code [categoryId, count]}; uncategorized lives excluded, busiest category first. */
    @Query("SELECT l.categoryId, COUNT(l) FROM Live l " +
           "WHERE l.status = :status AND l.categoryId IS NOT NULL " +
           "GROUP BY l.categoryId ORDER BY COUNT(l) DESC, l.categoryId ASC")
    List<Object[]> countByStatusGroupedByCategory(@Param("status") LiveStatus status);
}
