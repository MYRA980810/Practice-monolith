package com.livecomerce.cart.infrastructure.redis;

import com.livecomerce.cart.domain.CartLineKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryCartStoreAdapterTest {

    InMemoryCartStoreAdapter adapter;

    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID VARIANT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adapter = new InMemoryCartStoreAdapter();
    }

    @Test
    void addOrIncrement_newLine_createsLineAndIndexesStore() {
        adapter.addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 2);

        var cart = adapter.load(BUYER_ID, STORE_ID);
        assertThat(cart.findLine(new CartLineKey(PRODUCT_ID, VARIANT_ID)).orElseThrow().quantity()).isEqualTo(2);
        assertThat(adapter.loadStoreIds(BUYER_ID)).containsExactly(STORE_ID);
    }

    @Test
    void addOrIncrement_existingLine_incrementsNotDuplicates() {
        adapter.addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 2);
        adapter.addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 1);

        var cart = adapter.load(BUYER_ID, STORE_ID);
        assertThat(cart.lineCount()).isEqualTo(1);
        assertThat(cart.findLine(new CartLineKey(PRODUCT_ID, VARIANT_ID)).orElseThrow().quantity()).isEqualTo(3);
    }

    @Test
    void changeQuantity_positiveDelta_incrementsAndReturnsResult() {
        adapter.addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 2);

        int result = adapter.changeQuantity(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 1);

        assertThat(result).isEqualTo(3);
    }

    @Test
    void changeQuantity_toZero_removesLine_andCartBecomesEmpty() {
        adapter.addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 1);

        int result = adapter.changeQuantity(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, -1);

        assertThat(result).isZero();
        assertThat(adapter.load(BUYER_ID, STORE_ID).isEmpty()).isTrue();
        assertThat(adapter.loadStoreIds(BUYER_ID)).isEmpty();
    }

    @Test
    void removeLine_happyPath() {
        adapter.addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 1);

        adapter.removeLine(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID);

        assertThat(adapter.load(BUYER_ID, STORE_ID).isEmpty()).isTrue();
    }

    @Test
    void removeLine_idempotent_noOpOnAbsentItem() {
        adapter.removeLine(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID);
        adapter.removeLine(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID);

        assertThat(adapter.load(BUYER_ID, STORE_ID).isEmpty()).isTrue();
    }

    @Test
    void removeLine_lastLine_prunesFromIndex() {
        adapter.addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 1);

        adapter.removeLine(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID);

        assertThat(adapter.loadStoreIds(BUYER_ID)).isEmpty();
    }

    @Test
    void load_noData_returnsEmptyCart() {
        var cart = adapter.load(BUYER_ID, STORE_ID);

        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void loadStoreIds_multipleStores_isolatedPerBuyer() {
        var otherBuyer = UUID.randomUUID();
        var otherStore = UUID.randomUUID();
        adapter.addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 1);
        adapter.addOrIncrement(otherBuyer, otherStore, PRODUCT_ID, VARIANT_ID, 1);

        assertThat(adapter.loadStoreIds(BUYER_ID)).containsExactly(STORE_ID);
        assertThat(adapter.loadStoreIds(otherBuyer)).containsExactly(otherStore);
    }

    @Test
    void clear_deletesCartAndIndexEntry() {
        adapter.addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 1);

        adapter.clear(BUYER_ID, STORE_ID);

        assertThat(adapter.load(BUYER_ID, STORE_ID).isEmpty()).isTrue();
        assertThat(adapter.loadStoreIds(BUYER_ID)).isEmpty();
    }

    @Test
    void scanAllBuyerIds_returnsAllBuyersWithActiveCarts() {
        var otherBuyer = UUID.randomUUID();
        adapter.addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 1);
        adapter.addOrIncrement(otherBuyer, STORE_ID, PRODUCT_ID, VARIANT_ID, 1);

        assertThat(adapter.scanAllBuyerIds()).containsExactlyInAnyOrder(BUYER_ID, otherBuyer);
    }

    @Test
    void notifiedThreshold_roundTrip() {
        assertThat(adapter.loadNotifiedThreshold(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID)).isNull();

        adapter.recordNotifiedThreshold(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 5);

        assertThat(adapter.loadNotifiedThreshold(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID)).isEqualTo(5);
    }
}
