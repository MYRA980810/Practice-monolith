package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.GetStoreUseCase;
import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.domain.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetStoreService implements GetStoreUseCase {

    private final LoadStorePort loadStorePort;

    @Override
    public Store getByUserId(UUID userId) {
        return loadStorePort.loadByUserId(userId)
                .orElseThrow(() -> new StoreNotFoundException(userId.toString()));
    }

    @Override
    public Store getBySlug(String slug) {
        return loadStorePort.loadBySlug(slug)
                .orElseThrow(() -> new StoreNotFoundException(slug));
    }
}
