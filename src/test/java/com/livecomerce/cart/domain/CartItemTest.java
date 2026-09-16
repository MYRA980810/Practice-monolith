package com.livecomerce.cart.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartItemTest {

    private static CartLineKey aKey() {
        return new CartLineKey(UUID.randomUUID(), UUID.randomUUID());
    }

    @Test
    void of_withValidQuantity_succeeds() {
        var item = CartItem.of(aKey(), 2);

        assertThat(item.quantity()).isEqualTo(2);
    }

    @Test
    void of_withZeroQuantity_throws() {
        assertThatThrownBy(() -> CartItem.of(aKey(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void of_withNegativeQuantity_throws() {
        assertThatThrownBy(() -> CartItem.of(aKey(), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void of_withQuantityAtMax_succeeds() {
        var item = CartItem.of(aKey(), CartItem.MAX_QUANTITY);

        assertThat(item.quantity()).isEqualTo(CartItem.MAX_QUANTITY);
    }

    @Test
    void of_withQuantityExceedingMax_throws() {
        assertThatThrownBy(() -> CartItem.of(aKey(), CartItem.MAX_QUANTITY + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void of_withNullKey_throws() {
        assertThatThrownBy(() -> CartItem.of(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withQuantity_returnsNewInstanceWithUpdatedQuantityAndSameKey() {
        var key = aKey();
        var item = CartItem.of(key, 1);

        var incremented = item.withQuantity(3);

        assertThat(incremented.quantity()).isEqualTo(3);
        assertThat(incremented.key()).isEqualTo(key);
        assertThat(item.quantity()).isEqualTo(1); // original is unchanged (immutable)
    }

    @Test
    void withQuantity_violatingInvariant_throws() {
        var item = CartItem.of(aKey(), 1);

        assertThatThrownBy(() -> item.withQuantity(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
