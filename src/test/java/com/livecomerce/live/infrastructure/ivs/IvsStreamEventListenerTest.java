package com.livecomerce.live.infrastructure.ivs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IvsStreamEventListenerTest {

    @Mock LoadLivePort loadLivePort;
    @Mock SaveLivePort saveLivePort;

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
        live.start();
        when(loadLivePort.loadActiveByIvsChannelArn(CHANNEL_ARN)).thenReturn(Optional.of(live));

        var listener = new IvsStreamEventListener(loadLivePort, saveLivePort, objectMapper);
        var json = streamStateEventJson("Stream Start", CHANNEL_ARN);

        assertThatCode(() -> listener.onStreamStateEvent(json)).doesNotThrowAnyException();
        verify(saveLivePort).save(live);
    }

    @Test
    void streamStart_clearsPendingStreamEndedSignal() {
        var live = buildLive();
        live.start();
        live.markStreamEnded(java.time.Instant.now());
        when(loadLivePort.loadActiveByIvsChannelArn(CHANNEL_ARN)).thenReturn(Optional.of(live));

        var listener = new IvsStreamEventListener(loadLivePort, saveLivePort, objectMapper);
        var json = streamStateEventJson("Stream Start", CHANNEL_ARN);

        listener.onStreamStateEvent(json);

        assertThat(live.getStreamEndedAt()).isNull();
        verify(saveLivePort).save(live);
    }

    @Test
    void streamEnd_marksStreamEndedAt() {
        var live = buildLive();
        live.start();
        when(loadLivePort.loadActiveByIvsChannelArn(CHANNEL_ARN)).thenReturn(Optional.of(live));

        var listener = new IvsStreamEventListener(loadLivePort, saveLivePort, objectMapper);
        var json = streamStateEventJson("Stream End", CHANNEL_ARN);

        listener.onStreamStateEvent(json);

        assertThat(live.getStreamEndedAt()).isNotNull();
        verify(saveLivePort).save(live);
    }

    @Test
    void sessionEnded_marksStreamEndedAt() {
        var live = buildLive();
        live.start();
        when(loadLivePort.loadActiveByIvsChannelArn(CHANNEL_ARN)).thenReturn(Optional.of(live));

        var listener = new IvsStreamEventListener(loadLivePort, saveLivePort, objectMapper);
        var json = streamStateEventJson("Session Ended", CHANNEL_ARN);

        listener.onStreamStateEvent(json);

        assertThat(live.getStreamEndedAt()).isNotNull();
        verify(saveLivePort).save(live);
    }

    @Test
    void streamFailure_doesNotTouchStreamEndedAt() {
        var live = buildLive();
        live.start();
        when(loadLivePort.loadActiveByIvsChannelArn(CHANNEL_ARN)).thenReturn(Optional.of(live));

        var listener = new IvsStreamEventListener(loadLivePort, saveLivePort, objectMapper);
        var json = streamStateEventJson("Stream Failure", CHANNEL_ARN);

        listener.onStreamStateEvent(json);

        assertThat(live.getStreamEndedAt()).isNull();
        verify(saveLivePort, never()).save(live);
    }

    @Test
    void unknownChannelArn_logsWarning_andDoesNotThrow() {
        when(loadLivePort.loadActiveByIvsChannelArn(CHANNEL_ARN)).thenReturn(Optional.empty());

        var listener = new IvsStreamEventListener(loadLivePort, saveLivePort, objectMapper);
        var json = streamStateEventJson("Stream Start", CHANNEL_ARN);

        assertThatCode(() -> listener.onStreamStateEvent(json)).doesNotThrowAnyException();
    }

    @Test
    void malformedJson_isCaught_andDoesNotPropagate() {
        var listener = new IvsStreamEventListener(loadLivePort, saveLivePort, objectMapper);

        assertThatCode(() -> listener.onStreamStateEvent("{ not valid json"))
                .doesNotThrowAnyException();

        verifyNoInteractions(loadLivePort);
    }
}
