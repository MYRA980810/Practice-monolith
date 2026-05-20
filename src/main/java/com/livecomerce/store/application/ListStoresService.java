package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.ListStoresUseCase;
import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.domain.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListStoresService implements ListStoresUseCase {

    private final LoadStorePort loadStorePort;

    @Override
    public Page<Store> listActive(Pageable pageable) {
        return loadStorePort.loadAllActive(pageable);
    }
}
