package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.application.port.out.LoadProductVariantPort;
import com.livecomerce.catalog.application.port.out.SaveProductVariantPort;
import com.livecomerce.catalog.domain.ProductVariant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ProductVariantPersistenceAdapter implements LoadProductVariantPort, SaveProductVariantPort {

    private final ProductVariantJpaRepository repository;

    @Override
    @SuppressWarnings("null")
    public Optional<ProductVariant> loadById(UUID variantId) {
        return repository.findByIdWithStock(variantId);
    }

    @Override
    public List<ProductVariant> loadByProductId(UUID productId) {
        return repository.findByProductIdWithStock(productId);
    }

    @Override
    @SuppressWarnings("null")
    public ProductVariant save(ProductVariant variant) {
        return repository.save(variant);
    }
}
