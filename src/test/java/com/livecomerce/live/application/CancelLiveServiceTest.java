package com.livecomerce.live.application;

import com.livecomerce.live.LiveCancelledEvent;
import com.livecomerce.live.application.port.in.CancelLiveUseCase.CancelLiveCommand;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.LoadLiveSubscriptionPort;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.application.port.out.SaveLiveSubscriptionPort;
import com.livecomerce.live.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelLiveServiceTest {

    @Mock LoadLivePort              loadLivePort;
    @Mock SaveLivePort              saveLivePort;
    @Mock LoadLiveSubscriptionPort  loadLiveSubscriptionPort;
    @Mock SaveLiveSubscriptionPort  saveLiveSubscriptionPort;
    @Mock LiveRoomCloser            liveRoomCloser;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks CancelLiveService sut;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID STORE_ID  = UUID.randomUUID();

    @Test
    void cancelLive_fromScheduled_transitionsToCancelled() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = sut.cancelLive(new CancelLiveCommand(live.getId(), SELLER_ID));

        assertThat(result.getStatus()).isEqualTo(LiveStatus.CANCELLED);
    }

    @Test
    void cancelLive_fromEnded_throwsInvalidLiveState() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);
        live.start();
        live.end();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));

        assertThatThrownBy(() -> sut.cancelLive(new CancelLiveCommand(live.getId(), SELLER_ID)))
                .isInstanceOf(InvalidLiveStateException.class);
    }

    @Test
    void cancelLive_wrongSeller_throwsLiveNotOwned() {
        var live        = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);
        var wrongSeller = UUID.randomUUID();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));

        assertThatThrownBy(() -> sut.cancelLive(new CancelLiveCommand(live.getId(), wrongSeller)))
                .isInstanceOf(LiveNotOwnedBySellerException.class);
    }

    @Test
    @SuppressWarnings("null")
    void cancelLive_withSubscribers_publishesEventAndDeletesSubscriptions() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);
        var subscriber1 = UUID.randomUUID();
        var subscriber2 = UUID.randomUUID();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loadLiveSubscriptionPort.loadSubscriberIdsByLiveId(live.getId()))
                .thenReturn(List.of(subscriber1, subscriber2));

        sut.cancelLive(new CancelLiveCommand(live.getId(), SELLER_ID));

        var captor = ArgumentCaptor.forClass(LiveCancelledEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        var event = captor.getValue();
        assertThat(event.liveId()).isEqualTo(live.getId());
        assertThat(event.title()).isEqualTo("My Live");
        assertThat(event.subscriberIds()).containsExactlyInAnyOrder(subscriber1, subscriber2);
        assertThat(event.sellerId()).isEqualTo(SELLER_ID);
        assertThat(event.storeId()).isEqualTo(STORE_ID);
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.wasLive()).isFalse();
        verify(saveLiveSubscriptionPort).deleteAllByLiveId(live.getId());
    }

    @Test
    void cancelLive_withNoSubscribers_publishesEventWithEmptyList() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loadLiveSubscriptionPort.loadSubscriberIdsByLiveId(live.getId())).thenReturn(List.of());

        sut.cancelLive(new CancelLiveCommand(live.getId(), SELLER_ID));

        var captor = ArgumentCaptor.forClass(LiveCancelledEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().subscriberIds()).isEmpty();
        verify(saveLiveSubscriptionPort).deleteAllByLiveId(live.getId());
    }

    private Live startedLive() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);
        live.start();
        return live;
    }

    @Test
    void cancelLive_fromScheduled_doesNotCloseRoom() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);
        live.setIvsChannel("arn:ivs:channel", "rtmps://ingest", "arn:ivs:key", "sk_stream_key", "https://playback.url");
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.cancelLive(new CancelLiveCommand(live.getId(), SELLER_ID));

        verifyNoInteractions(liveRoomCloser);
    }

    @Test
    void cancelLive_fromLive_closesRoomAndFlagsEventAsWasLive() {
        var live = startedLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loadLiveSubscriptionPort.loadSubscriberIdsByLiveId(live.getId())).thenReturn(List.of());

        var result = sut.cancelLive(new CancelLiveCommand(live.getId(), SELLER_ID));

        assertThat(result.getStatus()).isEqualTo(LiveStatus.CANCELLED);
        verify(liveRoomCloser).closeRoom(live);
        var captor = ArgumentCaptor.forClass(LiveCancelledEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().wasLive()).isTrue();
        assertThat(captor.getValue().storeId()).isEqualTo(STORE_ID);
    }

    @Test
    void cancelLive_fromReconnecting_closesRoomAndFlagsEventAsWasLive() {
        var live = startedLive();
        live.beginReconnecting();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(saveLivePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loadLiveSubscriptionPort.loadSubscriberIdsByLiveId(live.getId())).thenReturn(List.of());

        sut.cancelLive(new CancelLiveCommand(live.getId(), SELLER_ID));

        verify(liveRoomCloser).closeRoom(live);
        var captor = ArgumentCaptor.forClass(LiveCancelledEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().wasLive()).isTrue();
    }
}
