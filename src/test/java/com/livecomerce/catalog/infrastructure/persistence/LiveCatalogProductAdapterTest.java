package com.livecomerce.catalog.infrastructure.persistence;

import com.livecomerce.catalog.domain.OptionType;
import com.livecomerce.catalog.domain.Product;
import com.livecomerce.live.LoadCatalogProductPort.LookupStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class LiveCatalogProductAdapterTest {

    private static final UUID STORE_ID = UUID.randomUUID();

    @Mock ProductJpaRepository repository;

    @InjectMocks LiveCatalogProductAdapter adapter;

    private static Product product() {
        return Product.create(STORE_ID, "T-Shirt", null, new BigDecimal("199.00"), "MXN", "SKU-1", null);
    }

    @Test
    void loadForLive_defaultVariant_snapshotsProductPriceAndCatalogData() {
        var product = product();
        product.addImage("https://cdn.test/shirt.jpg", null, null);
        var variantId = product.defaultVariant().getId();
        when(repository.findById(product.getId())).thenReturn(Optional.of(product));

        var lookup = adapter.loadForLive(product.getId(), variantId);
        assertThat(lookup.status()).isEqualTo(LookupStatus.FOUND);
        var snapshot = lookup.snapshot();

        assertThat(snapshot.productId()).isEqualTo(product.getId());
        assertThat(snapshot.variantId()).isEqualTo(variantId);
        assertThat(snapshot.storeId()).isEqualTo(STORE_ID);
        assertThat(snapshot.name()).isEqualTo("T-Shirt");
        assertThat(snapshot.price()).isEqualByComparingTo("199.00");
        assertThat(snapshot.currency()).isEqualTo("MXN");
        assertThat(snapshot.imageUrl()).isEqualTo("https://cdn.test/shirt.jpg");
        assertThat(snapshot.sellable()).isTrue();
    }

    @Test
    void loadForLive_variantWithPriceOverride_snapshotsVariantPrice() {
        var product = product();
        product.addOption("Size", OptionType.SIZE, List.of("M"));
        var variant = product.addVariant(Map.of("Size", "M"), "SKU-M", new BigDecimal("249.00"));
        when(repository.findById(product.getId())).thenReturn(Optional.of(product));

        var snapshot = adapter.loadForLive(product.getId(), variant.getId()).snapshot();

        assertThat(snapshot.variantId()).isEqualTo(variant.getId());
        assertThat(snapshot.price()).isEqualByComparingTo("249.00");
    }

    @Test
    void loadForLive_variantWithoutPriceOverride_fallsBackToProductPrice() {
        var product = product();
        product.addOption("Size", OptionType.SIZE, List.of("M"));
        var variant = product.addVariant(Map.of("Size", "M"), "SKU-M", null);
        when(repository.findById(product.getId())).thenReturn(Optional.of(product));

        var snapshot = adapter.loadForLive(product.getId(), variant.getId()).snapshot();

        assertThat(snapshot.price()).isEqualByComparingTo("199.00");
    }

    @Test
    void loadForLive_productWithoutImages_hasNullImageUrl() {
        var product = product();
        when(repository.findById(product.getId())).thenReturn(Optional.of(product));

        var snapshot = adapter.loadForLive(product.getId(), product.defaultVariant().getId()).snapshot();

        assertThat(snapshot.imageUrl()).isNull();
    }

    @Test
    void loadForLive_unknownProduct_returnsEmpty() {
        var productId = UUID.randomUUID();
        when(repository.findById(productId)).thenReturn(Optional.empty());

        assertThat(adapter.loadForLive(productId, UUID.randomUUID()).status()).isEqualTo(LookupStatus.PRODUCT_NOT_FOUND);
    }

    @Test
    void loadForLive_variantOfAnotherProduct_returnsEmpty() {
        var product = product();
        when(repository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThat(adapter.loadForLive(product.getId(), UUID.randomUUID()).status()).isEqualTo(LookupStatus.VARIANT_NOT_IN_PRODUCT);
    }

    @Test
    void loadForLive_pausedProduct_isNotSellable() {
        var product = product();
        product.pause();
        when(repository.findById(product.getId())).thenReturn(Optional.of(product));

        var snapshot = adapter.loadForLive(product.getId(), product.defaultVariant().getId()).snapshot();

        assertThat(snapshot.sellable()).isFalse();
    }

    @Test
    void loadForLive_deactivatedProduct_isNotSellable() {
        var product = product();
        product.deactivate();
        when(repository.findById(product.getId())).thenReturn(Optional.of(product));

        var snapshot = adapter.loadForLive(product.getId(), product.defaultVariant().getId()).snapshot();

        assertThat(snapshot.sellable()).isFalse();
    }
}
