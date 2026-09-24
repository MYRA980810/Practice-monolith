package com.livecomerce.catalog.application;

import com.livecomerce.catalog.LoadCartProductInfoPort;
import com.livecomerce.catalog.LoadLiveProductStatusPort;
import com.livecomerce.catalog.LoadLiveProductStatusPort.LiveProductBadge;
import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.domain.Product;
import com.livecomerce.catalog.domain.ProductImage;
import com.livecomerce.catalog.domain.ProductVariant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Composes {@link LoadProductPort} and {@link LoadLiveProductStatusPort} to
 * hydrate {@code cart}'s cart lines in a single batched call each.
 *
 * <p>D6 fail-closed contract: a failure resolving either signal for a line
 * excludes that line from the result — see {@link LoadCartProductInfoPort}
 * javadoc. This deliberately does NOT reuse {@code
 * GetProductService#loadLiveStatusSafely}'s fail-open default (empty map on
 * exception, badge defaults to NONE); that default is correct for a
 * read-only badge but wrong for a purchase-gating signal.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class CartProductInfoAdapter implements LoadCartProductInfoPort {

    private final LoadProductPort loadProductPort;
    private final LoadLiveProductStatusPort loadLiveProductStatusPort;

    @Override
    @Transactional(readOnly = true)
    public Map<CartLineRef, CartProductInfo> loadForCart(Collection<CartLineRef> refs) {
        if (refs.isEmpty()) return Map.of();

        Set<UUID> productIds = refs.stream().map(CartLineRef::productId).collect(Collectors.toSet());

        Map<UUID, Product> productsById = loadProductsSafely(productIds);
        if (productsById == null) {
            // Fail-closed: the price/stock signal could not be resolved for this
            // batch at all — every requested line in it is unavailable.
            return Map.of();
        }

        // null (as opposed to Map.of()) is the fail-closed sentinel here: an
        // empty map is a legitimate "no live association for any of these
        // products" answer from the port, but a thrown exception must NOT be
        // silently treated the same way (D6).
        Map<UUID, LiveProductBadge> liveStatuses = loadLiveStatusesSafely(productIds);

        Map<CartLineRef, CartProductInfo> result = new LinkedHashMap<>();
        for (CartLineRef ref : refs) {
            Product product = productsById.get(ref.productId());
            if (product == null) {
                log.warn("Cart line unavailable: product {} could not be resolved", ref.productId());
                continue;
            }

            ProductVariant variant = resolveVariant(product, ref.variantId());
            if (variant == null) {
                log.warn("Cart line unavailable: variant {} for product {} could not be resolved",
                        ref.variantId(), ref.productId());
                continue;
            }

            if (liveStatuses == null) {
                log.warn("Cart line unavailable: live-exclusivity status could not be resolved for product {}",
                        ref.productId());
                continue;
            }

            var badge = liveStatuses.getOrDefault(ref.productId(), LiveProductBadge.NONE);
            result.put(ref, toInfo(ref, product, variant, badge));
        }
        return result;
    }

    /**
     * @return the loaded products keyed by id, or {@code null} as a
     * fail-closed sentinel if the load itself threw.
     */
    private Map<UUID, Product> loadProductsSafely(Set<UUID> productIds) {
        try {
            return loadProductPort.loadByIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, Function.identity()));
        } catch (Exception e) {
            log.error("Failed to load {} products for cart hydration; failing closed (all lines unavailable)",
                    productIds.size(), e);
            return null;
        }
    }

    /**
     * @return the live statuses keyed by product id, or {@code null} as a
     * fail-closed sentinel if the load itself threw.
     */
    private Map<UUID, LiveProductBadge> loadLiveStatusesSafely(Set<UUID> productIds) {
        try {
            return loadLiveProductStatusPort.loadStatuses(productIds);
        } catch (Exception e) {
            log.error("Failed to load live status for {} products for cart hydration; failing closed (all lines unavailable)",
                    productIds.size(), e);
            return null;
        }
    }

    private ProductVariant resolveVariant(Product product, UUID variantId) {
        if (variantId == null) {
            return product.getVariants().stream()
                    .filter(ProductVariant::isDefault)
                    .findFirst()
                    .orElse(null);
        }
        return product.getVariants().stream()
                .filter(v -> v.getId().equals(variantId))
                .findFirst()
                .orElse(null);
    }

    private CartProductInfo toInfo(CartLineRef ref, Product product, ProductVariant variant, LiveProductBadge badge) {
        var stock = variant.getStock();
        return new CartProductInfo(
                product.getId(),
                variant.getId(),
                product.getStoreId(),
                product.getName(),
                primaryImageUrl(product),
                variant.effectivePrice(product.getBasePrice()),
                product.getCurrency(),
                stock != null ? stock.getAvailableQuantity() : 0,
                product.isActive(),
                product.isPaused(),
                badge.exclusiveToActiveLive()
        );
    }

    private String primaryImageUrl(Product product) {
        return product.getImages().stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .or(() -> product.getImages().stream().findFirst())
                .map(ProductImage::getUrl)
                .orElse(null);
    }
}
