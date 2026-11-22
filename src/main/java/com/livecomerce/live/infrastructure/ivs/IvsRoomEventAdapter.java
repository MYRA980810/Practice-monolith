package com.livecomerce.live.infrastructure.ivs;

import com.livecomerce.live.application.port.out.LiveRoomEventPort;
import com.livecomerce.live.application.port.out.RoomEvent;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.infrastructure.ChatProviderConditions;
import com.livecomerce.live.infrastructure.RoomEventFailureReporter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ivschat.IvschatClient;
import software.amazon.awssdk.services.ivschat.model.SendEventRequest;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Room events over IVS Chat {@code SendEvent} on the live's chat room: the event
 * type is the {@code eventName} and its attributes are sent as strings. Active
 * only when {@code live.chat-provider=ivs}. Requires {@code ivschat:SendEvent}.
 */
@Component
@Conditional(ChatProviderConditions.IvsSelected.class)
class IvsRoomEventAdapter implements LiveRoomEventPort {

    private static final Logger log = LoggerFactory.getLogger(IvsRoomEventAdapter.class);

    private final IvschatClient            ivschatClient;
    private final RoomEventFailureReporter failureReporter;

    @Autowired
    IvsRoomEventAdapter(IvschatClient ivschatClient, MeterRegistry meterRegistry) {
        this(ivschatClient, new RoomEventFailureReporter("ivs", log, meterRegistry, Clock.systemUTC()));
    }

    IvsRoomEventAdapter(IvschatClient ivschatClient, RoomEventFailureReporter failureReporter) {
        this.ivschatClient   = ivschatClient;
        this.failureReporter = failureReporter;
    }

    @Override
    public boolean publish(Live live, RoomEvent event) {
        if (live.getChatRoomArn() == null) {
            // Live started while the provider was agora: it has no IVS Chat room, so retrying is pointless.
            // DEBUG, not WARN: it fires on every event (viewer-count included) of such lives after a flip.
            log.debug("IVS Chat {} skipped: liveId={} has no chat room", event.type(), live.getId());
            return true;
        }
        try {
            ivschatClient.sendEvent(SendEventRequest.builder()
                    .roomIdentifier(live.getChatRoomArn())
                    .eventName(event.type())
                    .attributes(asStrings(event.attributes()))
                    .build());
            return true;
        } catch (RuntimeException e) { // SdkException (timeouts, throttling, AWS errors) and anything unexpected
            failureReporter.report(live.getId(), event.type(), e);
            return false;
        }
    }

    private static Map<String, String> asStrings(Map<String, Object> attributes) {
        var result = new LinkedHashMap<String, String>();
        attributes.forEach((key, value) -> result.put(key,
                value instanceof BigDecimal decimal ? decimal.toPlainString() : String.valueOf(value)));
        return result;
    }
}
