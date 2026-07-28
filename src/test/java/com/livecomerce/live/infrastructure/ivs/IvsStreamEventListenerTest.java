package com.livecomerce.live.infrastructure.ivs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IvsStreamEventListenerTest {

    @Mock LoadLivePort loadLivePort;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CHANNEL_ARN =
            "arn:aws:ivs:us-west-2:535011710559:channel/UCGaMPGLCbcE";

    private Live buildLive() {
        return Live.create(UUID.randomUUID(), UUID.randomUUID(), LiveContext.STORE,
                "Test Live", null, null, 60);
    }

    private String streamStateEventJson(String eventName, String channelArn) {
        return """
                {
                  "version": "0",
                  "id": "aa5b7a40-36cf-8dc4-5554-32d70e047215",
                  "detail-type": "IVS Stream State Change",
                  "source": "aws.ivs",
                  "account": "535011710559",
                  "time": "2024-09-09T16:17:26Z",
                  "region": "us-east-1",
                  "resources": ["%s"],
                  "detail": {
                    "event_name": "%s",
                    "channel_name": "",
                    "stream_id": "st-1AuTyMDASvHUTSb8p5PvbsO"
                  }
                }
                """.formatted(channelArn, eventName);
    }

    @Test
    void streamStart_resolvesLiveByChannelArn_andLogsWithoutThrowing() {
        var live = buildLive();
        when(loadLivePort.loadByIvsChannelArn(CHANNEL_ARN)).thenReturn(Optional.of(live));

        var listener = new IvsStreamEventListener(loadLivePort, objectMapper);
        var json = streamStateEventJson("Stream Start", CHANNEL_ARN);

        assertThatCode(() -> listener.onStreamStateEvent(json)).doesNotThrowAnyException();
    }

    @Test
    void unknownChannelArn_logsWarning_andDoesNotThrow() {
        when(loadLivePort.loadByIvsChannelArn(CHANNEL_ARN)).thenReturn(Optional.empty());

        var listener = new IvsStreamEventListener(loadLivePort, objectMapper);
        var json = streamStateEventJson("Stream Start", CHANNEL_ARN);

        assertThatCode(() -> listener.onStreamStateEvent(json)).doesNotThrowAnyException();
    }

    @Test
    void malformedJson_isCaught_andDoesNotPropagate() {
        var listener = new IvsStreamEventListener(loadLivePort, objectMapper);

        assertThatCode(() -> listener.onStreamStateEvent("{ not valid json"))
                .doesNotThrowAnyException();

        verifyNoInteractions(loadLivePort);
    }
}
