package com.livecomerce.store.application.port.out;

import com.livecomerce.store.domain.Store;

public interface SaveStorePort {

    Store save(Store store);
}
