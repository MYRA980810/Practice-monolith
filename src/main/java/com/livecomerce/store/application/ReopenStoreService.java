package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.ReopenStoreUseCase;
import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.application.port.out.SaveStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReopenStoreService implements ReopenStoreUseCase {

    private final LoadStorePort loadStorePort;
    private final SaveStorePort saveStorePort;

    @Override
    public void reopen(UUID userId) {
        var store = loadStorePort.loadByUserId(userId)
                .orElseThrow(() -> new StoreNotFoundException(userId.toString()));

        store.reopen();
        saveStorePort.save(store);
    }
}
