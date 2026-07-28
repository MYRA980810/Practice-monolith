package com.livecomerce.live.application.port.out;

public interface AgoraChatTokenPort {

    String generateRtmToken(String userId, int expiresInSeconds);

    String getAppId();
}
