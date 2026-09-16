package com.livecomerce.cart.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartLineKeyTest {

    @Test
    void sameProductAndVariant_areEqual() {
        var productId = UUID.randomUUID();
        var variantId = UUID.randomUUID();

        assertThat(new CartLineKey(productId, variantId))
                .isEqualTo(new CartLineKey(productId, variantId));
    }

    @Test
    void differentVariant_areNotEqual() {
        var productId = UUID.randomUUID();

        var a = new CartLineKey(productId, UUID.randomUUID());
        var b = new CartLineKey(productId, UUID.randomUUID());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void nullVariantId_isValidAndDistinctFromAnyRealVariant() {
        var productId = UUID.randomUUID();

        var noVariant = new CartLineKey(productId, null);
        var withVariant = new CartLineKey(productId, UUID.randomUUID());

        assertThat(noVariant.variantId()).isNull();
        assertThat(noVariant).isNotEqualTo(withVariant);
    }

    @Test
    void twoNullVariantKeysForSameProduct_areEqual() {
        var productId = UUID.randomUUID();

        assertThat(new CartLineKey(productId, null))
                .isEqualTo(new CartLineKey(productId, null));
    }

    @Test
    void nullProductId_throws() {
        assertThatThrownBy(() -> new CartLineKey(null, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
