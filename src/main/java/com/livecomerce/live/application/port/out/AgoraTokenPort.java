package com.livecomerce.live.application.port.out;

public interface AgoraTokenPort {

    String generateRtcToken(String channelName, String uid, int expiresInSeconds);

    String generateRtmToken(String userId, int expiresInSeconds);

    String getAppId();
}
