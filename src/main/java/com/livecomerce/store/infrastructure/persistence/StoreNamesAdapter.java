package com.livecomerce.store.infrastructure.persistence;

import com.livecomerce.live.LoadStoreNamesPort;
import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.domain.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class StoreNamesAdapter implements LoadStoreNamesPort {

    private final LoadStorePort loadStorePort;

    @Override
    public Map<UUID, String> loadNames(Collection<UUID> storeIds) {
        return loadStorePort.loadByIds(storeIds).stream()
                .collect(Collectors.toMap(Store::getId, Store::getName));
    }
}
