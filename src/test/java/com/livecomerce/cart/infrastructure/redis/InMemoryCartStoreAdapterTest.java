package com.livecomerce.cart.infrastructure.redis;

import com.livecomerce.cart.domain.CartLineKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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
    void addOrIncrement_concurrentRace_clampsToMaxQuantity_andSubsequentLoadDoesNotThrow() {
        // JD-R2-002/JDB2-006: two concurrent addToCart calls can each pass
        // the [1,99] check-then-act guard on a pre-race quantity and jointly
        // push the raw merge above CartItem.MAX_QUANTITY (99).
        adapter.addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 95);

        adapter.addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 10); // would overshoot to 105

        var cart = adapter.load(BUYER_ID, STORE_ID);
        assertThat(cart.findLine(new CartLineKey(PRODUCT_ID, VARIANT_ID)).orElseThrow().quantity()).isEqualTo(99);
        assertThatCode(() -> adapter.load(BUYER_ID, STORE_ID)).doesNotThrowAnyException();
    }

    @Test
    void changeQuantity_positiveDelta_incrementsAndReturnsResult() {
        adapter.addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 2);

        int result = adapter.changeQuantity(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 1, 10);

        assertThat(result).isEqualTo(3);
    }

    @Test
    void changeQuantity_toZero_removesLine_andCartBecomesEmpty() {
        adapter.addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 1);

        int result = adapter.changeQuantity(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, -1, null);

        assertThat(result).isZero();
        assertThat(adapter.load(BUYER_ID, STORE_ID).isEmpty()).isTrue();
        assertThat(adapter.loadStoreIds(BUYER_ID)).isEmpty();
    }

    @Test
    void changeQuantity_resultExceedsAvailableStock_clampsToAvailableStock() {
        adapter.addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 8);

        int result = adapter.changeQuantity(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 5, 10);

        assertThat(result).isEqualTo(10);
        assertThat(adapter.load(BUYER_ID, STORE_ID).findLine(new CartLineKey(PRODUCT_ID, VARIANT_ID))
                .orElseThrow().quantity()).isEqualTo(10);
    }

    @Test
    void changeQuantity_overshootCorrection_concurrentRemoveLine_doesNotResurrectLine() throws Exception {
        // JD-R2-001: simulate a concurrent removeLine racing in between the
        // main merge and the corrective one by intercepting the internal
        // map's mutating calls (put = old absolute-clamp primitive, merge
        // with a negative delta = new relative-correction primitive) and
        // deleting the entry right before the real call lands — exactly the
        // race window the bug/fix are about. Must never resurrect the line.
        adapter.addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 8);

        Class<?> lineKeyClass = Class.forName(
                "com.livecomerce.cart.infrastructure.redis.InMemoryCartStoreAdapter$LineKey");
        Constructor<?> ctor = lineKeyClass.getDeclaredConstructor(UUID.class, UUID.class, UUID.class, UUID.class);
        ctor.setAccessible(true);
        Object key = ctor.newInstance(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID);

        Field linesField = InMemoryCartStoreAdapter.class.getDeclaredField("lines");
        linesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<Object, Integer> realMap = (ConcurrentHashMap<Object, Integer>) linesField.get(adapter);

        ConcurrentHashMap<Object, Integer> racyMap = new ConcurrentHashMap<>(realMap) {
            @Override
            public Integer put(Object k, Integer value) {
                if (k.equals(key)) {
                    // old (buggy) implementation: an absolute clamp write —
                    // simulate the concurrent removal winning right before it
                    super.remove(k);
                }
                return super.put(k, value);
            }

            @Override
            public Integer merge(Object k, Integer value,
                    BiFunction<? super Integer, ? super Integer, ? extends Integer> fn) {
                if (k.equals(key) && value < 0) {
                    // new (fixed) implementation: a relative corrective
                    // merge — simulate the concurrent removal winning right
                    // before it, same as above
                    super.remove(k);
                }
                return super.merge(k, value, fn);
            }
        };
        linesField.set(adapter, racyMap);

        int result = adapter.changeQuantity(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 5, 10);

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

    @Test
    void clearNotifiedThreshold_removesRecordedValue() {
        adapter.recordNotifiedThreshold(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 5);

        adapter.clearNotifiedThreshold(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID);

        assertThat(adapter.loadNotifiedThreshold(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID)).isNull();
    }
}
