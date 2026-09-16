package com.livecomerce.catalog.application;

import com.livecomerce.catalog.LoadCartProductInfoPort.CartLineRef;
import com.livecomerce.catalog.LoadLiveProductStatusPort;
import com.livecomerce.catalog.LoadLiveProductStatusPort.LiveProductBadge;
import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.domain.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * D6 (confirmed 2026-09-15): fail-CLOSED on BOTH the price/stock signal and
 * the live-exclusivity signal. A failure resolving either one for a line
 * must make that line unavailable (absent from the result map) — never
 * defaulted to an available/non-exclusive state the way {@code
 * GetProductService#loadLiveStatusSafely} fails open for its display-only
 * badge.
 */
@ExtendWith(MockitoExtension.class)
class CartProductInfoAdapterTest {

    @Mock LoadProductPort loadProductPort;
    @Mock LoadLiveProductStatusPort loadLiveProductStatusPort;

    @InjectMocks CartProductInfoAdapter adapter;

    private static final UUID STORE_ID = UUID.randomUUID();

    private static Product buildProduct(String name, BigDecimal price) {
        return Product.create(STORE_ID, name, "desc", price, "MXN", "SKU-" + name, null);
    }

    @Test
    void loadForCart_happyPath_returnsHydratedInfoForEachLine() {
        var product = buildProduct("Playera", new BigDecimal("199.00"));
        product.defaultVariant().addStock(10);
        var ref = new CartLineRef(product.getId(), null);

        when(loadProductPort.loadByIds(Set.of(product.getId()))).thenReturn(List.of(product));
        when(loadLiveProductStatusPort.loadStatuses(Set.of(product.getId())))
                .thenReturn(Map.of(product.getId(), new LiveProductBadge(false, true)));

        var result = adapter.loadForCart(List.of(ref));

        assertThat(result).containsKey(ref);
        var info = result.get(ref);
        assertThat(info.productId()).isEqualTo(product.getId());
        assertThat(info.storeId()).isEqualTo(STORE_ID);
        assertThat(info.name()).isEqualTo("Playera");
        assertThat(info.unitPrice()).isEqualByComparingTo("199.00");
        assertThat(info.currency()).isEqualTo("MXN");
        assertThat(info.availableStock()).isEqualTo(10);
        assertThat(info.active()).isTrue();
        assertThat(info.paused()).isFalse();
        assertThat(info.exclusiveToActiveLive()).isTrue();
        assertThat(info.variantId()).isEqualTo(product.defaultVariant().getId());
    }

    @Test
    void loadForCart_withEmptyRefs_returnsEmptyMapWithoutQuerying() {
        var result = adapter.loadForCart(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void loadForCart_productMissingFromLoadResult_lineIsExcludedNotDefaulted() {
        var ref = new CartLineRef(UUID.randomUUID(), null);

        when(loadProductPort.loadByIds(any())).thenReturn(List.of());
        when(loadLiveProductStatusPort.loadStatuses(any())).thenReturn(Map.of());

        var result = adapter.loadForCart(List.of(ref));

        assertThat(result).doesNotContainKey(ref);
    }

    @Test
    void loadForCart_whenPriceStockLookupThrows_failsClosed_allLinesExcluded() {
        var ref = new CartLineRef(UUID.randomUUID(), null);

        when(loadProductPort.loadByIds(any())).thenThrow(new RuntimeException("catalog db down"));

        var result = adapter.loadForCart(List.of(ref));

        assertThat(result).isEmpty();
    }

    @Test
    void loadForCart_whenLiveStatusLookupThrows_failsClosed_lineExcludedNotDefaultedToNonExclusive() {
        var product = buildProduct("Playera", new BigDecimal("199.00"));
        product.defaultVariant().addStock(10);
        var ref = new CartLineRef(product.getId(), null);

        when(loadProductPort.loadByIds(any())).thenReturn(List.of(product));
        when(loadLiveProductStatusPort.loadStatuses(any())).thenThrow(new RuntimeException("live status db down"));

        var result = adapter.loadForCart(List.of(ref));

        // Must NOT be present with exclusiveToActiveLive defaulted to false —
        // the whole line must be unavailable, per D6.
        assertThat(result).doesNotContainKey(ref);
    }

    @Test
    void loadForCart_perLineIsolation_oneFailingLineDoesNotAffectSiblings() {
        var productA = buildProduct("A", BigDecimal.TEN);
        var productB = buildProduct("B", BigDecimal.TEN);
        var missingProductId = UUID.randomUUID(); // simulates a product that failed to resolve
        productA.defaultVariant().addStock(5);
        productB.defaultVariant().addStock(5);

        var refA = new CartLineRef(productA.getId(), null);
        var refB = new CartLineRef(productB.getId(), null);
        var refMissing = new CartLineRef(missingProductId, null);

        when(loadProductPort.loadByIds(Set.of(productA.getId(), productB.getId(), missingProductId)))
                .thenReturn(List.of(productA, productB)); // missing one is simply absent, not thrown
        when(loadLiveProductStatusPort.loadStatuses(any())).thenReturn(Map.of());

        var result = adapter.loadForCart(List.of(refA, refB, refMissing));

        assertThat(result).containsKeys(refA, refB);
        assertThat(result).doesNotContainKey(refMissing);
        assertThat(result.get(refA).name()).isEqualTo("A");
        assertThat(result.get(refB).name()).isEqualTo("B");
    }

    @Test
    void loadForCart_whenVariantIdNotFoundOnProduct_lineIsExcluded() {
        var product = buildProduct("Playera", new BigDecimal("199.00"));
        var unknownVariantId = UUID.randomUUID();
        var ref = new CartLineRef(product.getId(), unknownVariantId);

        when(loadProductPort.loadByIds(any())).thenReturn(List.of(product));
        when(loadLiveProductStatusPort.loadStatuses(any())).thenReturn(Map.of());

        var result = adapter.loadForCart(List.of(ref));

        assertThat(result).doesNotContainKey(ref);
    }

    @Test
    void loadForCart_missingLiveStatusEntryForProduct_defaultsToNone() {
        var product = buildProduct("Playera", new BigDecimal("199.00"));
        product.defaultVariant().addStock(3);
        var ref = new CartLineRef(product.getId(), null);

        when(loadProductPort.loadByIds(any())).thenReturn(List.of(product));
        when(loadLiveProductStatusPort.loadStatuses(any())).thenReturn(Map.of()); // no entry for this product

        var result = adapter.loadForCart(List.of(ref));

        assertThat(result.get(ref).exclusiveToActiveLive()).isFalse();
    }
}
