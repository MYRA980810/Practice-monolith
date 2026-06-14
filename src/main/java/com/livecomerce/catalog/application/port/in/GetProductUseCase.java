package com.livecomerce.catalog.application.port.in;

import com.livecomerce.catalog.application.query.ProductView;

import java.util.List;
import java.util.UUID;

public interface GetProductUseCase {

    ProductView getById(UUID productId);

    List<ProductView> getByStoreId(UUID storeId);

    List<ProductView> listWithFilters(ProductFilter filter);
}
