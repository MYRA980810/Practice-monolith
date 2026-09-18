package com.livecomerce.cart.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A buyer's cart for a single store — the in-memory shape of {@code
 * cart:{buyerId}:{storeId}}, the Redis hash added by a later PR in this
 * change. Pure domain: no Spring, no Redis. Reconstituted from whatever the
 * storage adapter loaded and holds only the invariants that must never be
 * violated regardless of adapter: per-line quantity bounds (delegated to
 * {@link CartItem}) and line identity (at most one {@link CartItem} per
 * {@link CartLineKey} — lines are merged, never duplicated).
 */
public final class Cart {

    private final UUID buyerId;
    private final UUID storeId;
    private final Map<CartLineKey, CartItem> lines;

    private Cart(UUID buyerId, UUID storeId, Map<CartLineKey, CartItem> lines) {
        this.buyerId = buyerId;
        this.storeId = storeId;
        this.lines = lines;
    }

    public static Cart empty(UUID buyerId, UUID storeId) {
        return new Cart(requireBuyer(buyerId), requireStore(storeId), new LinkedHashMap<>());
    }

    public static Cart of(UUID buyerId, UUID storeId, Collection<CartItem> items) {
        var lines = new LinkedHashMap<CartLineKey, CartItem>();
        for (CartItem item : items) {
            if (lines.containsKey(item.key())) {
                throw new IllegalArgumentException(
                        "Duplicate cart line for key " + item.key()
                                + " — lines must be merged before reconstitution, not duplicated");
            }
            lines.put(item.key(), item);
        }
        return new Cart(requireBuyer(buyerId), requireStore(storeId), lines);
    }

    private static UUID requireBuyer(UUID buyerId) {
        if (buyerId == null) {
            throw new IllegalArgumentException("buyerId must not be null");
        }
        return buyerId;
    }

    private static UUID requireStore(UUID storeId) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId must not be null");
        }
        return storeId;
    }

    public UUID buyerId() {
        return buyerId;
    }

    public UUID storeId() {
        return storeId;
    }

    public Optional<CartItem> findLine(CartLineKey key) {
        return Optional.ofNullable(lines.get(key));
    }

    public Collection<CartItem> items() {
        return Collections.unmodifiableCollection(lines.values());
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public int lineCount() {
        return lines.size();
    }
}
