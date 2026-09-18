package com.livecomerce.cart.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();

    @Test
    void empty_hasNoLines() {
        var cart = Cart.empty(BUYER_ID, STORE_ID);

        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.lineCount()).isZero();
        assertThat(cart.buyerId()).isEqualTo(BUYER_ID);
        assertThat(cart.storeId()).isEqualTo(STORE_ID);
    }

    @Test
    void of_withUniqueLines_succeeds() {
        var itemA = CartItem.of(new CartLineKey(UUID.randomUUID(), null), 1);
        var itemB = CartItem.of(new CartLineKey(UUID.randomUUID(), null), 2);

        var cart = Cart.of(BUYER_ID, STORE_ID, List.of(itemA, itemB));

        assertThat(cart.lineCount()).isEqualTo(2);
        assertThat(cart.items()).containsExactlyInAnyOrder(itemA, itemB);
    }

    @Test
    void of_withDuplicateLineKey_throws() {
        var key = new CartLineKey(UUID.randomUUID(), UUID.randomUUID());
        var first = CartItem.of(key, 1);
        var duplicate = CartItem.of(key, 2);

        assertThatThrownBy(() -> Cart.of(BUYER_ID, STORE_ID, List.of(first, duplicate)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findLine_returnsPresentLine() {
        var key = new CartLineKey(UUID.randomUUID(), null);
        var item = CartItem.of(key, 1);
        var cart = Cart.of(BUYER_ID, STORE_ID, List.of(item));

        assertThat(cart.findLine(key)).contains(item);
    }

    @Test
    void findLine_returnsEmptyForAbsentLine() {
        var cart = Cart.empty(BUYER_ID, STORE_ID);

        assertThat(cart.findLine(new CartLineKey(UUID.randomUUID(), null))).isEmpty();
    }

    @Test
    void of_withNullBuyerId_throws() {
        assertThatThrownBy(() -> Cart.of(null, STORE_ID, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void of_withNullStoreId_throws() {
        assertThatThrownBy(() -> Cart.of(BUYER_ID, null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void items_isUnmodifiable() {
        var cart = Cart.empty(BUYER_ID, STORE_ID);

        assertThatThrownBy(() -> cart.items().add(CartItem.of(new CartLineKey(UUID.randomUUID(), null), 1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
