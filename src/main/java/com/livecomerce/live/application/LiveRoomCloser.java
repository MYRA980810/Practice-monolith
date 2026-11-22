package com.livecomerce.live.application;

import com.livecomerce.live.application.port.out.ChatRoomPort;
import com.livecomerce.live.application.port.out.LiveRoomEventPort;
import com.livecomerce.live.application.port.out.RoomEvent;
import com.livecomerce.live.application.port.out.VideoBroadcastPort;
import com.livecomerce.live.domain.Live;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Tears down the live room once a live stops being on air (ended or cancelled
 * while LIVE/RECONNECTING): tells everyone in the live room the live ended, then stops the IVS stream and deletes the IVS Chat room. Every step is
 * best-effort; a failure is logged and never blocks the state change that
 * triggered the close.
 */
@Component
@RequiredArgsConstructor
class LiveRoomCloser {

    private static final Logger log = LoggerFactory.getLogger(LiveRoomCloser.class);

    private final LiveRoomEventPort   liveRoomEventPort;
    private final VideoBroadcastPort  videoBroadcastPort;
    private final ChatRoomPort        chatRoomPort;

    void closeRoom(Live live) {
        liveRoomEventPort.publish(live, new RoomEvent.LiveEnded(live.getId()));

        if (live.getIvsChannelArn() != null) {
            try {
                videoBroadcastPort.stopStream(live.getIvsChannelArn());
            } catch (Exception e) {
                log.warn("IVS stopStream failed: liveId={}, channelArn={}, error={}",
                        live.getId(), live.getIvsChannelArn(), e.getMessage());
            }
        }

        if (live.getChatRoomArn() != null) {
            try {
                chatRoomPort.deleteRoom(live.getChatRoomArn());
            } catch (Exception e) {
                log.warn("IVS Chat deleteRoom failed: liveId={}, roomArn={}, error={}",
                        live.getId(), live.getChatRoomArn(), e.getMessage());
            }
        }
    }
}
