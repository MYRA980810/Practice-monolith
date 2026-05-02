package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.application.port.out.SaveProductPort;
import com.livecomerce.catalog.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ProductPersistenceAdapter implements LoadProductPort, SaveProductPort {

    private final ProductJpaRepository repository;

    @Override
    public Optional<Product> loadById(UUID productId) {
        return repository.findByIdWithDetails(productId);
    }

    @Override
    public List<Product> loadByStoreId(UUID storeId) {
        return repository.findByStoreIdActiveWithDetails(storeId);
    }

    @Override
    @SuppressWarnings("null")
    public Product save(Product product) {
        return repository.save(product);
    }
}
