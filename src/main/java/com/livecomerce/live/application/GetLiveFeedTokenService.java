package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.GetLiveFeedTokenUseCase;
import com.livecomerce.live.application.port.out.AgoraChatTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Issues an RTM login token for the global {@link LiveFeedBroadcastListener#FEED_CHANNEL}
 * channel — unlike {@link GetChatTokenService}, there is no live to look up: any
 * authenticated user browsing the feed can request one. The token itself is
 * login-scoped only (no channel binding), so it's the same kind of credential
 * as the per-live chat token, just not tied to a liveId/status check.
 */
@Service
@RequiredArgsConstructor
public class GetLiveFeedTokenService implements GetLiveFeedTokenUseCase {

    private static final int TOKEN_TTL_SECONDS = 3600;

    private final AgoraChatTokenPort agoraTokenPort;

    @Override
    public FeedTokenResult getFeedToken(UUID userId) {
        var token = agoraTokenPort.generateRtmToken(userId.toString(), TOKEN_TTL_SECONDS);
        var appId = agoraTokenPort.getAppId();

        return new FeedTokenResult(token, LiveFeedBroadcastListener.FEED_CHANNEL, appId);
    }
}
