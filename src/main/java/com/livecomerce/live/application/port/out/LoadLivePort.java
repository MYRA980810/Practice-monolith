package com.livecomerce.live.application.port.out;

import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadLivePort {

    Optional<Live> loadById(UUID id);

    Optional<Live> loadByAgoraChannelId(String agoraChannelId);

    List<Live> loadBySellerId(UUID sellerId);

    List<Live> loadBySellerIdAndStatus(UUID sellerId, LiveStatus status);
}
