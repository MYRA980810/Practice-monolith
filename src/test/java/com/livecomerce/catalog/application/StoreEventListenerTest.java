package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.out.SaveProductPort;
import com.livecomerce.store.StoreDeactivatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StoreEventListenerTest {

    @Mock SaveProductPort saveProductPort;

    @InjectMocks StoreEventListener listener;

    private static final UUID STORE_ID = UUID.randomUUID();

    @Test
    void on_storeDeactivatedEvent_deactivatesAllProductsForThatStore() {
        listener.on(new StoreDeactivatedEvent(STORE_ID));

        verify(saveProductPort).deactivateAllByStoreId(STORE_ID);
    }
}
