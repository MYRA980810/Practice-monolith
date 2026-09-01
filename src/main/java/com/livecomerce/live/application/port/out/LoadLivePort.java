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

    /** Lives still marked LIVE whose stream-ended signal is older than {@code cutoff}. */
    List<Live> loadStaleLive(Instant cutoff);
}
