package com.livecomerce.catalog.application.port.out;

import com.livecomerce.catalog.domain.ProductVariant;

public interface SaveProductVariantPort {

    ProductVariant save(ProductVariant variant);
}
