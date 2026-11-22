package com.livecomerce.live.infrastructure.agora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.port.out.AgoraRtmMessagePort;
import com.livecomerce.live.application.port.out.LiveRoomEventPort;
import com.livecomerce.live.application.port.out.RoomEvent;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.infrastructure.ChatProviderConditions;
import com.livecomerce.live.infrastructure.RoomEventFailureReporter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.LinkedHashMap;

/**
 * Room events over Agora RTM: a JSON message {@code {"type": ..., <attributes>}}
 * on channel {@code live-chat:{liveId}}. Active unless {@code live.chat-provider=ivs}.
 */
@Component
@Conditional(ChatProviderConditions.AgoraSelected.class)
class AgoraRoomEventAdapter implements LiveRoomEventPort {

    private final AgoraRtmMessagePort      agoraRtmMessagePort;
    private final ObjectMapper             objectMapper;
    private final RoomEventFailureReporter failureReporter;

    @Autowired
    AgoraRoomEventAdapter(AgoraRtmMessagePort agoraRtmMessagePort, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this(agoraRtmMessagePort, objectMapper, new RoomEventFailureReporter(
                "agora", LoggerFactory.getLogger(AgoraRoomEventAdapter.class), meterRegistry, Clock.systemUTC()));
    }

    AgoraRoomEventAdapter(AgoraRtmMessagePort agoraRtmMessagePort, ObjectMapper objectMapper,
                          RoomEventFailureReporter failureReporter) {
        this.agoraRtmMessagePort = agoraRtmMessagePort;
        this.objectMapper        = objectMapper;
        this.failureReporter     = failureReporter;
    }

    @Override
    public boolean publish(Live live, RoomEvent event) {
        try {
            var payload = new LinkedHashMap<String, Object>();
            payload.put("type", event.type());
            payload.putAll(event.attributes());
            // The REST adapter already logs HTTP failures; false here only signals "not delivered".
            return agoraRtmMessagePort.sendChannelMessage("live-chat:" + live.getId(), objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            failureReporter.report(live.getId(), event.type(), e);
            return false;
        }
    }
}
