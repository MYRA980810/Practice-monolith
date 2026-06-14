package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.out.SaveProductPort;
import com.livecomerce.store.StoreDeactivatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class StoreEventListener {

    private final SaveProductPort saveProductPort;

    @ApplicationModuleListener
    void on(StoreDeactivatedEvent event) {
        saveProductPort.deactivateAllByStoreId(event.storeId());
    }
}
