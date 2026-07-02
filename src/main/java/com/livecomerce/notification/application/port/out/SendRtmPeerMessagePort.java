package com.livecomerce.notification.application.port.out;

public interface SendRtmPeerMessagePort {

    void sendPeerMessage(String userId, String payload);
}
