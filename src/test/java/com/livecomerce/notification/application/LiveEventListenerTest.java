package com.livecomerce.notification.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.LiveCancelledEvent;
import com.livecomerce.live.LiveStartedEvent;
import com.livecomerce.notification.application.port.out.SaveNotificationPort;
import com.livecomerce.notification.application.port.out.SendRtmPeerMessagePort;
import com.livecomerce.notification.domain.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveEventListenerTest {

    @Mock SaveNotificationPort    saveNotificationPort;
    @Mock SendRtmPeerMessagePort  sendRtmPeerMessagePort;
    @Spy  ObjectMapper            objectMapper = new ObjectMapper();
    @InjectMocks LiveEventListener sut;

    private static final UUID LIVE_ID  = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();

    @Test
    @SuppressWarnings("null")
    void onLiveStarted_withSubscribers_savesNotificationsAndSendsPeerMessages() {
        var subscriber1 = UUID.randomUUID();
        var subscriber2 = UUID.randomUUID();
        var event = new LiveStartedEvent(LIVE_ID, STORE_ID, "Flash Sale", List.of(subscriber1, subscriber2));
        when(saveNotificationPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.on(event);

        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(saveNotificationPort, times(2)).save(captor.capture());
        var notifications = captor.getAllValues();
        assertThat(notifications).extracting(Notification::getType).containsOnly("live-started");
        assertThat(notifications).extracting(Notification::getLiveId).containsOnly(LIVE_ID);
        assertThat(notifications).extracting(Notification::getUserId)
                .containsExactlyInAnyOrder(subscriber1, subscriber2);

        verify(sendRtmPeerMessagePort, times(2)).sendPeerMessage(anyString(), anyString());
        verify(sendRtmPeerMessagePort).sendPeerMessage(eq(subscriber1.toString()), anyString());
        verify(sendRtmPeerMessagePort).sendPeerMessage(eq(subscriber2.toString()), anyString());
    }

    @Test
    void onLiveStarted_withNoSubscribers_doesNothing() {
        var event = new LiveStartedEvent(LIVE_ID, STORE_ID, "Empty", List.of());

        sut.on(event);

        verify(saveNotificationPort, never()).save(any());
        verify(sendRtmPeerMessagePort, never()).sendPeerMessage(anyString(), anyString());
    }

    @Test
    @SuppressWarnings("null")
    void onLiveCancelled_withSubscribers_savesNotificationsAndSendsPeerMessages() {
        var subscriber1 = UUID.randomUUID();
        var event = new LiveCancelledEvent(LIVE_ID, "My Show", List.of(subscriber1));
        when(saveNotificationPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.on(event);

        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(saveNotificationPort).save(captor.capture());
        var notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo("live-cancelled");
        assertThat(notification.getLiveId()).isEqualTo(LIVE_ID);
        assertThat(notification.getUserId()).isEqualTo(subscriber1);

        verify(sendRtmPeerMessagePort).sendPeerMessage(eq(subscriber1.toString()), anyString());
    }

    @Test
    void onLiveCancelled_withNoSubscribers_doesNothing() {
        var event = new LiveCancelledEvent(LIVE_ID, "Empty", List.of());

        sut.on(event);

        verify(saveNotificationPort, never()).save(any());
        verify(sendRtmPeerMessagePort, never()).sendPeerMessage(anyString(), anyString());
    }
}
