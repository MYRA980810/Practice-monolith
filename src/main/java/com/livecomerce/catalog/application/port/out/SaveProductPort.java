package com.livecomerce.catalog.application.port.out;

import com.livecomerce.catalog.domain.Product;

import java.util.UUID;

public interface SaveProductPort {

    Product save(Product product);

    void deactivateAllByStoreId(UUID storeId);
}
