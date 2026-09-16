package com.livecomerce.cart.application.port.out;

import com.livecomerce.cart.domain.Cart;

import java.util.Set;
import java.util.UUID;

/**
 * Intent-level out-port for the buyer's per-store cart storage (design D1) —
 * same shape as {@code live.application.port.out.ViewerCountPort}: callers
 * express what they want done, adapters own the storage technology (Redis
 * hash + set in production, an in-memory map under the {@code local}
 * profile). No repository-style CRUD leaks through.
 *
 * <p>{@link #changeQuantity} and {@link #removeLine} both converge on the
 * same "delete the line once it would be zero or fewer" behaviour (design
 * D7) and both cascade to deleting the whole per-store cart (and pruning it
 * from the buyer's cart-store index) once it becomes empty — callers never
 * need to check for that themselves.
 */
public interface CartStorePort {

    /**
     * Adds a new line or increments an existing one by {@code quantity}
     * (always positive — this is the "add to cart" op, not an arbitrary
     * delta). Refreshes the per-store cart's TTL and the buyer's cart-store
     * index membership + TTL (design D5).
     */
    void addOrIncrement(UUID buyerId, UUID storeId, UUID productId, UUID variantId, int quantity);

    /**
     * Applies {@code delta} (positive or negative) to an existing line's
     * quantity. If the resulting quantity is at or below zero, the line is
     * removed (design D7) — callers must not pre-check for this themselves,
     * but MUST validate any upper bound (e.g. available stock) before
     * calling this method, since the underlying counter op is a raw
     * increment with no bound-check of its own.
     *
     * @return the resulting quantity, or {@code 0} if the line was removed
     * as a result of this call.
     */
    int changeQuantity(UUID buyerId, UUID storeId, UUID productId, UUID variantId, int delta);

    /**
     * Removes a line. Idempotent — a no-op (not an error) if the line is
     * already absent. If this was the cart's last line, the whole per-store
     * cart is deleted and pruned from the buyer's cart-store index.
     */
    void removeLine(UUID buyerId, UUID storeId, UUID productId, UUID variantId);

    /**
     * Loads the current state of a buyer's cart for one store. Never
     * {@code null} — returns {@link Cart#empty} if nothing is stored.
     */
    Cart load(UUID buyerId, UUID storeId);

    /**
     * Lists the store ids the buyer has an active cart in. Self-heals stale
     * index entries as a side effect of this read (design D5): any storeId
     * present in the index but whose per-store cart has since expired/emptied
     * is pruned from the index before returning, so every id returned here is
     * guaranteed to resolve to a non-empty {@link #load}.
     */
    Set<UUID> loadStoreIds(UUID buyerId);

    /** Deletes a buyer's whole per-store cart and its index membership. */
    void clear(UUID buyerId, UUID storeId);

    /**
     * Lists every buyer id that currently has at least one active cart.
     * Used only by {@code CartStockDepletionJob} to enumerate carts to
     * sweep — implementations MUST use a non-blocking scan (Redis {@code
     * SCAN}), never a blocking full key listing (Redis {@code KEYS}).
     */
    Set<UUID> scanAllBuyerIds();

    /**
     * Reads back the last stock-depletion threshold notified for a line, or
     * {@code null} if none has been recorded yet — the dedupe state behind
     * design D9's "once per distinct threshold crossed" rule.
     */
    Integer loadNotifiedThreshold(UUID buyerId, UUID storeId, UUID productId, UUID variantId);

    /**
     * Records the last stock-depletion threshold notified for a line, with a
     * TTL matching the cart's own (design D9).
     */
    void recordNotifiedThreshold(UUID buyerId, UUID storeId, UUID productId, UUID variantId, int threshold);
}
