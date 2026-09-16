package com.livecomerce.cart.domain;

import java.util.Objects;

/**
 * A single cart line: a {@link CartLineKey} plus a quantity. Enforces the
 * quantity invariants (at least 1, capped at {@link #MAX_QUANTITY}) so no
 * caller — application service or storage adapter — can construct or
 * reconstitute an invalid line. Pure Java: no Spring, no persistence
 * annotations.
 */
public final class CartItem {

    public static final int MAX_QUANTITY = 99;

    private final CartLineKey key;
    private final int quantity;

    private CartItem(CartLineKey key, int quantity) {
        this.key = key;
        this.quantity = quantity;
    }

    public static CartItem of(CartLineKey key, int quantity) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        validateQuantity(quantity);
        return new CartItem(key, quantity);
    }

    public CartItem withQuantity(int newQuantity) {
        validateQuantity(newQuantity);
        return new CartItem(this.key, newQuantity);
    }

    private static void validateQuantity(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1, was: " + quantity);
        }
        if (quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity cannot exceed " + MAX_QUANTITY + ", was: " + quantity);
        }
    }

    public CartLineKey key() {
        return key;
    }

    public int quantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CartItem other)) return false;
        return quantity == other.quantity && key.equals(other.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, quantity);
    }

    @Override
    public String toString() {
        return "CartItem{key=" + key + ", quantity=" + quantity + "}";
    }
}
