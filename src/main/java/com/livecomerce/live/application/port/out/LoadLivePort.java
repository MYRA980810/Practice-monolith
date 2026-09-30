package com.livecomerce.live.application.port.out;

import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadLivePort {

    Optional<Live> loadById(UUID id);

    Optional<Live> loadByAgoraChannelId(String agoraChannelId);

    Optional<Live> loadActiveByIvsChannelArn(String ivsChannelArn);

    List<Live> loadBySellerId(UUID sellerId);

    List<Live> loadBySellerIdAndStatus(UUID sellerId, LiveStatus status);

    List<Live> loadByStoreId(UUID storeId);

    List<Live> loadByStoreIdAndStatus(UUID storeId, LiveStatus status);

    Page<Live> loadByStatus(LiveStatus status, Pageable pageable);

    Page<Live> loadUpcoming(Pageable pageable);

    /** Same as {@link #loadByStatus} but restricted to one category; uncategorized lives are excluded. */
    Page<Live> loadByStatusAndCategory(LiveStatus status, UUID categoryId, Pageable pageable);

    /** Same as {@link #loadUpcoming} but restricted to one category; uncategorized lives are excluded. */
    Page<Live> loadUpcomingByCategory(UUID categoryId, Pageable pageable);

    /** Lives still marked LIVE whose stream-ended signal is older than {@code cutoff}. */
    List<Live> loadStaleLive(Instant cutoff);

    /** Lives in RECONNECTING whose reconnection window has expired — second timeout. */
    List<Live> loadStaleReconnecting(Instant cutoff);

    /**
     * Number of lives in {@code status} per category; uncategorized lives are excluded.
     * Ordered by count descending, ties broken by category id ascending.
     */
    List<CategoryLiveCount> countByStatusGroupedByCategory(LiveStatus status);

    /**
     * Number of upcoming lives per category, using the same filter as {@link #loadUpcoming}
     * (SCHEDULED with a scheduled date); uncategorized lives are excluded.
     * Ordered by count descending, ties broken by category id ascending.
     */
    List<CategoryLiveCount> countUpcomingGroupedByCategory();

    record CategoryLiveCount(UUID categoryId, long count) {}
}
