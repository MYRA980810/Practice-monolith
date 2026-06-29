package com.livecomerce.live.application.port.out;

public interface AgoraRtmMessagePort {

    void sendChannelMessage(String channelName, String payloadJson);

    void sendPeerMessage(String userId, String payloadJson);
}
