package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.application.port.in.ProductFilter;
import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.application.port.out.SaveProductPort;
import com.livecomerce.catalog.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Collection;
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
        List<Product> products = repository.findByStoreIdActiveWithDetails(storeId);
        // Hydrate remaining lazy collections within the same session so callers
        // with open-in-view=false don't hit LazyInitializationException
        repository.findByStoreIdActiveWithImages(storeId);
        repository.findByStoreIdActiveWithOptions(storeId);
        repository.findByStoreIdActiveWithVariantOptionValues(storeId);
        return products;
    }

    @Override
    public List<Product> loadByFilter(ProductFilter filter) {
        Specification<Product> spec = ProductSpecification.hasStoreId(filter.storeId())
                .and(ProductSpecification.isActive())
                .and(ProductSpecification.isNotPaused());

        if (filter.categoryId() != null)
            spec = spec.and(ProductSpecification.hasCategoryId(filter.categoryId()));

        spec = switch (filter.stockLevel()) {
            case CRITICAL -> spec.and(ProductSpecification.hasCriticalStock());
            case NORMAL   -> spec.and(ProductSpecification.hasNormalStock());
            case ALL      -> spec;
        };

        Sort sort = switch (filter.sortBy()) {
            case PRICE_ASC      -> Sort.by("basePrice").ascending();
            case PRICE_DESC     -> Sort.by("basePrice").descending();
            case RECENTLY_ADDED -> Sort.by("createdAt").descending();
        };

        List<Product> products = repository.findAll(spec, sort);
        if (products.isEmpty()) return products;

        List<UUID> ids = products.stream().map(Product::getId).toList();
        repository.findByIdsWithVariantsAndStock(ids);
        repository.findByIdsWithImages(ids);
        repository.findByIdsWithOptions(ids);
        repository.findByIdsWithVariantOptionValues(ids);

        return products;
    }

    @Override
    public List<Product> loadByIds(Collection<UUID> productIds) {
        if (productIds.isEmpty()) return List.of();

        List<UUID> ids = List.copyOf(productIds);
        List<Product> products = repository.findByIdsWithVariantsAndStock(ids);
        if (products.isEmpty()) return products;

        // Hydrate remaining lazy collections within the same session so callers
        // with open-in-view=false don't hit LazyInitializationException — same
        // pattern as loadByFilter/browsePublic. A single bulk query for the
        // whole id list, never one query per product.
        repository.findByIdsWithImages(ids);

        return products;
    }

    @Override
    public Page<Product> browsePublic(ProductFilter filter, Pageable pageable) {
        Specification<Product> spec = ProductSpecification.isActive()
                .and(ProductSpecification.isNotPaused());

        if (filter.categoryId() != null)
            spec = spec.and(ProductSpecification.hasCategoryId(filter.categoryId()));

        Sort sort = switch (filter.sortBy()) {
            case PRICE_ASC      -> Sort.by("basePrice").ascending();
            case PRICE_DESC     -> Sort.by("basePrice").descending();
            case RECENTLY_ADDED -> Sort.by("createdAt").descending();
        };

        Page<Product> page = repository.findAll(spec, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort));
        List<Product> products = page.getContent();
        if (products.isEmpty()) return page;

        // Hydrate remaining lazy collections within the same session so callers
        // with open-in-view=false don't hit LazyInitializationException
        List<UUID> ids = products.stream().map(Product::getId).toList();
        repository.findByIdsWithVariantsAndStock(ids);
        repository.findByIdsWithImages(ids);
        repository.findByIdsWithOptions(ids);
        repository.findByIdsWithVariantOptionValues(ids);

        return page;
    }

    @Override
    @SuppressWarnings("null")
    public Product save(Product product) {
        return repository.save(product);
    }

    @Override
    public void deactivateAllByStoreId(UUID storeId) {
        repository.deactivateAllByStoreId(storeId, OffsetDateTime.now());
    }
}
