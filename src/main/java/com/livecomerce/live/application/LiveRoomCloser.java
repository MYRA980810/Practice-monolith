package com.livecomerce.live.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.port.out.AgoraRtmMessagePort;
import com.livecomerce.live.application.port.out.VideoBroadcastPort;
import com.livecomerce.live.domain.Live;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Tears down the live room once a live stops being on air (ended or cancelled
 * while LIVE/RECONNECTING): tells everyone in {@code live-chat:{id}} the live
 * ended, then stops the IVS stream. Both steps are best-effort; a failure is
 * logged and never blocks the state change that triggered the close.
 */
@Component
@RequiredArgsConstructor
class LiveRoomCloser {

    private static final Logger log = LoggerFactory.getLogger(LiveRoomCloser.class);

    private final AgoraRtmMessagePort agoraRtmMessagePort;
    private final VideoBroadcastPort  videoBroadcastPort;
    private final ObjectMapper        objectMapper;

    void closeRoom(Live live) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "type",   "live-ended",
                    "liveId", live.getId()
            ));
            agoraRtmMessagePort.sendChannelMessage("live-chat:" + live.getId(), payload);
        } catch (Exception e) {
            log.warn("Agora RTM live-ended failed: {}", e.getMessage());
        }

        if (live.getIvsChannelArn() != null) {
            try {
                videoBroadcastPort.stopStream(live.getIvsChannelArn());
            } catch (Exception e) {
                log.warn("IVS stopStream failed: liveId={}, channelArn={}, error={}",
                        live.getId(), live.getIvsChannelArn(), e.getMessage());
            }
        }
    }
}
