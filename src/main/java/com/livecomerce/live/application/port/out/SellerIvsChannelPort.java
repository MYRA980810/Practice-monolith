package com.livecomerce.live.application.port.out;

import com.livecomerce.live.domain.SellerIvsChannel;

import java.util.Optional;
import java.util.UUID;

public interface SellerIvsChannelPort {

    Optional<SellerIvsChannel> loadBySellerId(UUID sellerId);

    boolean trySave(SellerIvsChannel channel);
}
