package com.livecomerce.catalog.application.port.in;

import com.livecomerce.catalog.domain.Product;

import java.util.List;
import java.util.UUID;

public interface GetProductUseCase {

    Product getById(UUID productId);

    List<Product> getByStoreId(UUID storeId);
}
