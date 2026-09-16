package com.livecomerce.store.application;

import com.livecomerce.live.LiveEndedEvent;
import com.livecomerce.live.LiveStartedEvent;
import com.livecomerce.store.application.port.out.SaveStoreLiveStatusPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StoreLiveStatusEventListenerTest {

    @Mock SaveStoreLiveStatusPort saveStoreLiveStatusPort;

    @InjectMocks StoreLiveStatusEventListener listener;

    @Test
    void onLiveStartedEvent_withStoreId_marksStoreLive() {
        var storeId = UUID.randomUUID();
        var liveId = UUID.randomUUID();
        var occurredAt = Instant.now();
        var event = new LiveStartedEvent(liveId, storeId, "Título", List.of(), occurredAt);

        listener.on(event);

        verify(saveStoreLiveStatusPort).markLive(storeId, liveId, occurredAt);
    }

    @Test
    void onLiveStartedEvent_withoutStoreId_doesNothing() {
        // SELLER_PROFILE-context lives carry no storeId — nothing to project.
        var event = new LiveStartedEvent(UUID.randomUUID(), null, "Título", List.of(), Instant.now());

        listener.on(event);

        verify(saveStoreLiveStatusPort, never()).markLive(any(), any(), any());
    }

    @Test
    void onLiveEndedEvent_withStoreId_marksLiveEnded() {
        var storeId = UUID.randomUUID();
        var liveId = UUID.randomUUID();
        var occurredAt = Instant.now();
        var event = new LiveEndedEvent(liveId, UUID.randomUUID(), storeId, occurredAt);

        listener.on(event);

        verify(saveStoreLiveStatusPort).markEnded(storeId, liveId, occurredAt);
    }

    @Test
    void onLiveEndedEvent_withoutStoreId_doesNothing() {
        var event = new LiveEndedEvent(UUID.randomUUID(), UUID.randomUUID(), null, Instant.now());

        listener.on(event);

        verify(saveStoreLiveStatusPort, never()).markEnded(any(), any(), any());
    }
}
