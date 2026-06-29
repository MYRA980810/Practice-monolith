package com.livecomerce.live.infrastructure.agora;

import com.livecomerce.live.application.port.out.AgoraTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AgoraTokenAdapter implements AgoraTokenPort {

    @Value("${agora.app-id}")
    private String appId;

    @Value("${agora.app-certificate}")
    private String appCertificate;

    @Override
    public String generateRtcToken(String channelName, String uid, int expiresInSeconds) {
        return AgoraRtcTokenBuilder.buildToken(appId, appCertificate, channelName, uid, expiresInSeconds);
    }

    @Override
    public String generateRtmToken(String userId, int expiresInSeconds) {
        return AgoraRtmTokenBuilder.buildTokenWithUserAccount(appId, appCertificate, userId, expiresInSeconds);
    }

    @Override
    public String getAppId() {
        return appId;
    }
}
