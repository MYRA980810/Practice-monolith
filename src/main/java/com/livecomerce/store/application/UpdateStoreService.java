package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.UpdateStoreUseCase;
import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.application.port.out.SaveStorePort;
import com.livecomerce.store.domain.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateStoreService implements UpdateStoreUseCase {

    private final LoadStorePort loadStorePort;
    private final SaveStorePort saveStorePort;

    @Override
    public Store update(UpdateStoreCommand command) {
        Store store = loadStorePort.loadByUserId(command.userId())
                .orElseThrow(() -> new StoreNotFoundException(command.userId().toString()));

        store.updateProfile(command.name(), command.description(), command.logoUrl());

        return saveStorePort.save(store);
    }
}
