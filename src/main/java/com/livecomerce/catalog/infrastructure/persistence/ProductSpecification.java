package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.domain.Product;
import com.livecomerce.catalog.domain.ProductVariant;
import com.livecomerce.catalog.domain.Stock;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

final class ProductSpecification {

    static final int CRITICAL_THRESHOLD = 5;

    private ProductSpecification() {}

    static Specification<Product> hasStoreId(UUID storeId) {
        return (root, query, cb) -> cb.equal(root.get("storeId"), storeId);
    }

    static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    static Specification<Product> isNotPaused() {
        return (root, query, cb) -> cb.isFalse(root.get("paused"));
    }

    static Specification<Product> hasCategoryId(UUID categoryId) {
        return (root, query, cb) -> cb.equal(root.get("categoryId"), categoryId);
    }

    @SuppressWarnings("null")
    static Specification<Product> hasCriticalStock() {
        return (root, query, cb) -> {
            Subquery<UUID> sub = query.subquery(UUID.class);
            Root<ProductVariant> v = sub.from(ProductVariant.class);
            Join<ProductVariant, Stock> s = v.join("stock", JoinType.INNER);
            sub.select(v.get("id"))
               .where(cb.and(
                   cb.equal(v.get("product"), root),
                   cb.lessThanOrEqualTo(s.get("availableQuantity"), CRITICAL_THRESHOLD)
               ));
            return cb.exists(sub);
        };
    }

    @SuppressWarnings("null")
    static Specification<Product> hasNormalStock() {
        return (root, query, cb) -> {
            Subquery<UUID> sub = query.subquery(UUID.class);
            Root<ProductVariant> v = sub.from(ProductVariant.class);
            Join<ProductVariant, Stock> s = v.join("stock", JoinType.INNER);
            sub.select(v.get("id"))
               .where(cb.and(
                   cb.equal(v.get("product"), root),
                   cb.lessThanOrEqualTo(s.get("availableQuantity"), CRITICAL_THRESHOLD)
               ));
            return cb.not(cb.exists(sub));
        };
    }
}
