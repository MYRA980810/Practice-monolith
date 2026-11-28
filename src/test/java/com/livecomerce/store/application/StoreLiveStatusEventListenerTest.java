package com.livecomerce.store.application;

import com.livecomerce.live.LiveCancelledEvent;
import com.livecomerce.live.LiveEndedEvent;
import com.livecomerce.live.LiveStartedEvent;
import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.application.port.out.SaveStoreLiveStatusPort;
import com.livecomerce.store.domain.Store;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreLiveStatusEventListenerTest {

    @Mock SaveStoreLiveStatusPort saveStoreLiveStatusPort;
    @Mock LoadStorePort loadStorePort;

    @InjectMocks StoreLiveStatusEventListener listener;

    private UUID givenSellerOwnsStore(UUID sellerId) {
        var storeId = UUID.randomUUID();
        var store = mock(Store.class);
        when(store.getId()).thenReturn(storeId);
        when(loadStorePort.loadByUserId(sellerId)).thenReturn(Optional.of(store));
        return storeId;
    }

    @Test
    void onLiveStartedEvent_sellerWithStore_marksTheSellersStoreLive() {
        var sellerId = UUID.randomUUID();
        var storeId = givenSellerOwnsStore(sellerId);
        var liveId = UUID.randomUUID();
        var occurredAt = Instant.now();

        listener.on(new LiveStartedEvent(liveId, sellerId, "Title", List.of(), occurredAt));

        verify(saveStoreLiveStatusPort).markLive(storeId, liveId, occurredAt);
    }

    @Test
    void onLiveStartedEvent_sellerWithoutStore_doesNothing() {
        var sellerId = UUID.randomUUID();
        when(loadStorePort.loadByUserId(sellerId)).thenReturn(Optional.empty());

        listener.on(new LiveStartedEvent(UUID.randomUUID(), sellerId, "Title", List.of(), Instant.now()));

        verify(saveStoreLiveStatusPort, never()).markLive(any(), any(), any());
    }

    @Test
    void onLiveEndedEvent_sellerWithStore_marksLiveEnded() {
        var sellerId = UUID.randomUUID();
        var storeId = givenSellerOwnsStore(sellerId);
        var liveId = UUID.randomUUID();
        var occurredAt = Instant.now();

        listener.on(new LiveEndedEvent(liveId, sellerId, occurredAt));

        verify(saveStoreLiveStatusPort).markEnded(storeId, liveId, occurredAt);
    }

    @Test
    void onLiveEndedEvent_sellerWithoutStore_doesNothing() {
        var sellerId = UUID.randomUUID();
        when(loadStorePort.loadByUserId(sellerId)).thenReturn(Optional.empty());

        listener.on(new LiveEndedEvent(UUID.randomUUID(), sellerId, Instant.now()));

        verify(saveStoreLiveStatusPort, never()).markEnded(any(), any(), any());
    }

    @Test
    void onLiveCancelledEvent_whenWasLive_sellerWithStore_marksLiveEnded() {
        var sellerId = UUID.randomUUID();
        var storeId = givenSellerOwnsStore(sellerId);
        var liveId = UUID.randomUUID();
        var occurredAt = Instant.now();

        listener.on(new LiveCancelledEvent(liveId, sellerId, "Title", List.of(), true, occurredAt));

        verify(saveStoreLiveStatusPort).markEnded(storeId, liveId, occurredAt);
    }

    @Test
    void onLiveCancelledEvent_whenNotLive_doesNothing() {
        // A SCHEDULED live never marked the store live, so there is nothing to clear.
        listener.on(new LiveCancelledEvent(UUID.randomUUID(), UUID.randomUUID(),
                "Title", List.of(), false, Instant.now()));

        verifyNoInteractions(loadStorePort, saveStoreLiveStatusPort);
    }

    @Test
    void onLiveCancelledEvent_whenWasLive_sellerWithoutStore_doesNothing() {
        var sellerId = UUID.randomUUID();
        when(loadStorePort.loadByUserId(sellerId)).thenReturn(Optional.empty());

        listener.on(new LiveCancelledEvent(UUID.randomUUID(), sellerId,
                "Title", List.of(), true, Instant.now()));

        verify(saveStoreLiveStatusPort, never()).markEnded(any(), any(), any());
    }
}
