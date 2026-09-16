package com.livecomerce.cart.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCartStoreAdapterTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock HashOperations<String, String, String> hashOps;
    @Mock SetOperations<String, String> setOps;
    @Mock ValueOperations<String, String> valueOps;

    RedisCartStoreAdapter adapter;

    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID VARIANT_ID = UUID.randomUUID();

    private static final String CART_KEY = "cart:" + BUYER_ID + ":" + STORE_ID;
    private static final String INDEX_KEY = "buyer:" + BUYER_ID + ":cart-stores";
    private static final String FIELD_WITH_VARIANT = PRODUCT_ID + ":" + VARIANT_ID;
    private static final String FIELD_NO_VARIANT = PRODUCT_ID + ":none";

    @BeforeEach
    void setUp() {
        adapter = new RedisCartStoreAdapter(redisTemplate);
    }

    @Test
    void addOrIncrement_incrementsHashField_addsToIndex_refreshesTtlOnBothKeys() {
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);

        adapter.addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 2);

        verify(hashOps).increment(CART_KEY, FIELD_WITH_VARIANT, 2L);
        verify(setOps).add(INDEX_KEY, STORE_ID.toString());
        verify(redisTemplate).expire(CART_KEY, Duration.ofDays(7));
        verify(redisTemplate).expire(INDEX_KEY, Duration.ofDays(7));
    }

    @Test
    void addOrIncrement_nullVariant_usesNoneField() {
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);

        adapter.addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, null, 1);

        verify(hashOps).increment(CART_KEY, FIELD_NO_VARIANT, 1L);
    }

    @Test
    void changeQuantity_positiveDelta_incrementsAndReturnsResult() {
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(hashOps.increment(CART_KEY, FIELD_WITH_VARIANT, 1L)).thenReturn(3L);

        int result = adapter.changeQuantity(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 1);

        assertThat(result).isEqualTo(3);
        verify(hashOps, never()).delete(any(), any());
    }

    @Test
    void changeQuantity_resultAtOrBelowZero_deletesLine_andReturnsZero() {
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(hashOps.increment(CART_KEY, FIELD_WITH_VARIANT, -1L)).thenReturn(0L);
        when(hashOps.size(CART_KEY)).thenReturn(0L);

        int result = adapter.changeQuantity(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, -1);

        assertThat(result).isZero();
        verify(hashOps).delete(CART_KEY, FIELD_WITH_VARIANT);
    }

    @Test
    void changeQuantity_lastLineRemoved_deletesCartAndPrunesIndex() {
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(hashOps.increment(CART_KEY, FIELD_WITH_VARIANT, -1L)).thenReturn(0L);
        when(hashOps.size(CART_KEY)).thenReturn(0L);

        adapter.changeQuantity(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, -1);

        verify(redisTemplate).delete(CART_KEY);
        verify(setOps).remove(INDEX_KEY, STORE_ID.toString());
    }

    @Test
    void removeLine_deletesField() {
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOps);
        when(hashOps.size(CART_KEY)).thenReturn(1L);

        adapter.removeLine(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID);

        verify(hashOps).delete(CART_KEY, FIELD_WITH_VARIANT);
    }

    @Test
    void removeLine_lastLine_alsoDeletesCartAndPrunesIndex() {
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(hashOps.size(CART_KEY)).thenReturn(0L);

        adapter.removeLine(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID);

        verify(redisTemplate).delete(CART_KEY);
        verify(setOps).remove(INDEX_KEY, STORE_ID.toString());
    }

    @Test
    void load_reconstitutesCartFromHash() {
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOps);
        when(hashOps.entries(CART_KEY)).thenReturn(Map.of(FIELD_WITH_VARIANT, "3", FIELD_NO_VARIANT, "1"));

        var cart = adapter.load(BUYER_ID, STORE_ID);

        assertThat(cart.lineCount()).isEqualTo(2);
        assertThat(cart.findLine(new com.livecomerce.cart.domain.CartLineKey(PRODUCT_ID, VARIANT_ID))
                .orElseThrow().quantity()).isEqualTo(3);
        assertThat(cart.findLine(new com.livecomerce.cart.domain.CartLineKey(PRODUCT_ID, null))
                .orElseThrow().quantity()).isEqualTo(1);
    }

    @Test
    void load_emptyHash_returnsEmptyCart() {
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOps);
        when(hashOps.entries(CART_KEY)).thenReturn(Map.of());

        var cart = adapter.load(BUYER_ID, STORE_ID);

        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void loadStoreIds_returnsIndexMembers_whenAllHashesNonEmpty() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOps);
        when(setOps.members(INDEX_KEY)).thenReturn(Set.of(STORE_ID.toString()));
        when(hashOps.size(CART_KEY)).thenReturn(2L);

        var result = adapter.loadStoreIds(BUYER_ID);

        assertThat(result).containsExactly(STORE_ID);
        verify(setOps, never()).remove(eq(INDEX_KEY), any());
    }

    @Test
    void loadStoreIds_staleEntry_selfHealsByPruningFromIndex() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOps);
        when(setOps.members(INDEX_KEY)).thenReturn(Set.of(STORE_ID.toString()));
        when(hashOps.size(CART_KEY)).thenReturn(0L); // hash expired/empty, index still lists it

        var result = adapter.loadStoreIds(BUYER_ID);

        assertThat(result).isEmpty();
        verify(setOps).remove(INDEX_KEY, STORE_ID.toString());
    }

    @Test
    void clear_deletesCartAndIndexEntry() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);

        adapter.clear(BUYER_ID, STORE_ID);

        verify(redisTemplate).delete(CART_KEY);
        verify(setOps).remove(INDEX_KEY, STORE_ID.toString());
    }

    @Test
    void notifiedThreshold_roundTrip() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        String key = "cart:" + BUYER_ID + ":" + STORE_ID + ":notified:" + PRODUCT_ID + ":" + VARIANT_ID;
        when(valueOps.get(key)).thenReturn("5");

        var result = adapter.loadNotifiedThreshold(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void notifiedThreshold_absent_returnsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        String key = "cart:" + BUYER_ID + ":" + STORE_ID + ":notified:" + PRODUCT_ID + ":" + VARIANT_ID;
        when(valueOps.get(key)).thenReturn(null);

        var result = adapter.loadNotifiedThreshold(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID);

        assertThat(result).isNull();
    }

    @Test
    void recordNotifiedThreshold_setsValueWithCartTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        String key = "cart:" + BUYER_ID + ":" + STORE_ID + ":notified:" + PRODUCT_ID + ":" + VARIANT_ID;

        adapter.recordNotifiedThreshold(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 5);

        verify(valueOps).set(key, "5", Duration.ofDays(7));
    }
}
