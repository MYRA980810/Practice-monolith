package com.livecomerce.live.application.port.out;

public interface AgoraRtmMessagePort {

    /** Best-effort; never throws. Returns {@code false} when Agora did not accept the message. */
    boolean sendChannelMessage(String channelName, String payloadJson);

    void sendPeerMessage(String userId, String payloadJson);
}
