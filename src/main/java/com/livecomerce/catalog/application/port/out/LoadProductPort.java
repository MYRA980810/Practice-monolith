package com.livecomerce.catalog.application.port.out;

import com.livecomerce.catalog.domain.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadProductPort {

    Optional<Product> loadById(UUID productId);

    List<Product> loadByStoreId(UUID storeId);
}
