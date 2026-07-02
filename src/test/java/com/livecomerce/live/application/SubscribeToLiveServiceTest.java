package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.SubscribeToLiveUseCase.SubscribeToLiveCommand;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.LoadLiveSubscriptionPort;
import com.livecomerce.live.application.port.out.SaveLiveSubscriptionPort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveContext;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotScheduledException;
import com.livecomerce.live.domain.LiveSubscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscribeToLiveServiceTest {

    @Mock LoadLivePort             loadLivePort;
    @Mock LoadLiveSubscriptionPort loadLiveSubscriptionPort;
    @Mock SaveLiveSubscriptionPort saveLiveSubscriptionPort;
    @InjectMocks SubscribeToLiveService sut;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID BUYER_ID  = UUID.randomUUID();

    private static Live scheduledLive() {
        return Live.create(SELLER_ID, null, LiveContext.SELLER_PROFILE, "Live", null, null, 60);
    }

    private static Live liveLive() {
        var live = Live.create(SELLER_ID, null, LiveContext.SELLER_PROFILE, "Live", null, null, 60);
        live.start();
        return live;
    }

    private static Live endedLive() {
        var live = Live.create(SELLER_ID, null, LiveContext.SELLER_PROFILE, "Live", null, null, 60);
        live.start();
        live.end();
        return live;
    }

    @Test
    void subscribe_toScheduledLive_savesSubscription() {
        var live = scheduledLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(loadLiveSubscriptionPort.existsByLiveIdAndBuyerId(live.getId(), BUYER_ID)).thenReturn(false);

        sut.subscribe(new SubscribeToLiveCommand(live.getId(), BUYER_ID));

        verify(saveLiveSubscriptionPort).save(any(LiveSubscription.class));
    }

    @Test
    void subscribe_toLiveLive_throwsLiveNotScheduledException() {
        var live = liveLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));

        assertThatThrownBy(() -> sut.subscribe(new SubscribeToLiveCommand(live.getId(), BUYER_ID)))
                .isInstanceOf(LiveNotScheduledException.class);

        verify(saveLiveSubscriptionPort, never()).save(any());
    }

    @Test
    void subscribe_toEndedLive_throwsLiveNotScheduledException() {
        var live = endedLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));

        assertThatThrownBy(() -> sut.subscribe(new SubscribeToLiveCommand(live.getId(), BUYER_ID)))
                .isInstanceOf(LiveNotScheduledException.class);

        verify(saveLiveSubscriptionPort, never()).save(any());
    }

    @Test
    void subscribe_whenAlreadySubscribed_isIdempotentAndSkipsSave() {
        var live = scheduledLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(loadLiveSubscriptionPort.existsByLiveIdAndBuyerId(live.getId(), BUYER_ID)).thenReturn(true);

        sut.subscribe(new SubscribeToLiveCommand(live.getId(), BUYER_ID));

        verify(saveLiveSubscriptionPort, never()).save(any());
    }

    @Test
    void subscribe_toScheduledLive_withAnyBuyerId_noSellerCheckPerformed() {
        // The service does NOT check if buyerId belongs to a buyer role —
        // that guard lives in the controller. Any UUID is accepted by the service.
        var live = scheduledLive();
        var anyUserId = UUID.randomUUID();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(loadLiveSubscriptionPort.existsByLiveIdAndBuyerId(live.getId(), anyUserId)).thenReturn(false);

        sut.subscribe(new SubscribeToLiveCommand(live.getId(), anyUserId));

        verify(saveLiveSubscriptionPort).save(any(LiveSubscription.class));
    }
}
