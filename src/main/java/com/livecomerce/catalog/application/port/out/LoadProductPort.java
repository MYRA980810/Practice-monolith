package com.livecomerce.catalog.application.port.out;

import com.livecomerce.catalog.application.port.in.ProductFilter;
import com.livecomerce.catalog.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadProductPort {

    Optional<Product> loadById(UUID productId);

    List<Product> loadByStoreId(UUID storeId);

    List<Product> loadByFilter(ProductFilter filter);

    /**
     * Bulk load for cross-module batch hydration (e.g. {@code
     * catalog.application.CartProductInfoAdapter} resolving N cart lines in
     * one round trip). Issues a single query for the whole {@code
     * productIds} batch, never one query per id.
     */
    List<Product> loadByIds(Collection<UUID> productIds);

    /**
     * Global catalog browse for the public storefront: active, non-paused
     * products across ALL stores, optionally scoped to a category. Unlike
     * {@link #loadByFilter}, this never scopes by store — {@code filter.storeId()}
     * is ignored entirely.
     */
    Page<Product> browsePublic(ProductFilter filter, Pageable pageable);
}
