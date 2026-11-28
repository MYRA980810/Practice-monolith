package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.domain.Product;
import com.livecomerce.catalog.domain.ProductImage;
import com.livecomerce.catalog.domain.ProductVariant;
import com.livecomerce.live.LoadCatalogProductPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class LiveCatalogProductAdapter implements LoadCatalogProductPort {

    private final ProductJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("null") // UUID is non-null by contract; JPA findById expects @NonNull
    public CatalogProductLookup loadForLive(UUID productId, UUID variantId) {
        var product = repository.findById(productId).orElse(null);
        if (product == null) {
            return CatalogProductLookup.productNotFound();
        }
        return product.getVariants().stream()
                .filter(v -> v.getId().equals(variantId))
                .findFirst()
                .map(variant -> CatalogProductLookup.found(toSnapshot(product, variant)))
                .orElseGet(CatalogProductLookup::variantNotInProduct);
    }

    private static CatalogProductSnapshot toSnapshot(Product product, ProductVariant variant) {
        return new CatalogProductSnapshot(
                product.getId(),
                variant.getId(),
                product.getStoreId(),
                product.getName(),
                variant.effectivePrice(product.getBasePrice()),
                product.getCurrency(),
                primaryImageUrl(product),
                product.isActive(),
                product.isPaused());
    }

    private static String primaryImageUrl(Product product) {
        return product.getImages().stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .or(() -> product.getImages().stream().findFirst())
                .map(ProductImage::getUrl)
                .orElse(null);
    }
}
