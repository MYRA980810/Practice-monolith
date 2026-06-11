package com.livecomerce.catalog.application.port.out;

import com.livecomerce.catalog.domain.ProductVariant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadProductVariantPort {

    Optional<ProductVariant> loadById(UUID variantId);

    List<ProductVariant> loadByProductId(UUID productId);
}
