package com.livecomerce.live.application.port.in;

import java.util.UUID;

public interface GetLiveFeedTokenUseCase {

    FeedTokenResult getFeedToken(UUID userId);

    record FeedTokenResult(String token, String channelName, String appId) {}
}
