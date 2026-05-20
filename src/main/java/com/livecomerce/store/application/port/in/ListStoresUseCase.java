package com.livecomerce.store.application.port.in;

import com.livecomerce.store.domain.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListStoresUseCase {

    Page<Store> listActive(Pageable pageable);
}
