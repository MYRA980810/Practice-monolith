package com.livecomerce.cart.application;

import com.livecomerce.cart.CartCheckoutCompletedEvent;
import com.livecomerce.cart.application.port.in.CheckoutCartUseCase;
import com.livecomerce.cart.application.port.out.CartStorePort;
import com.livecomerce.cart.domain.Cart;
import com.livecomerce.cart.domain.CartLineKey;
import com.livecomerce.catalog.LoadCartProductInfoPort;
import com.livecomerce.catalog.LoadCartProductInfoPort.CartLineRef;
import com.livecomerce.catalog.LoadCartProductInfoPort.CartProductInfo;
import com.livecomerce.order.PlaceCartOrderPort;
import com.livecomerce.order.PlaceCartOrderPort.CartOrderLine;
import com.livecomerce.order.PlaceCartOrderPort.PlaceCartOrderCommand;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Batches selected item ids into one {@link PlaceCartOrderPort} call per
 * store, best-effort across stores (design D8). {@code order}'s batch
 * entry point cannot report which line in a batch failed with insufficient
 * stock (JDB-002, PR1 review), so every line's availability — stock,
 * live-exclusivity, and D6 fail-closed catalog-lookup failures — is
 * validated here, against the same freshly-fetched {@link CartProductInfo}
 * used for hydration, BEFORE {@code order} is ever called. {@code order}
 * therefore only ever receives lines this class has already confirmed
 * should succeed, and its own transaction stays all-or-nothing for exactly
 * that pre-vetted batch.
 */
@Service
@RequiredArgsConstructor
public class CheckoutCartService implements CheckoutCartUseCase {

    private static final Logger log = LoggerFactory.getLogger(CheckoutCartService.class);

    private static final String REASON_OUT_OF_STOCK = "OUT_OF_STOCK";
    private static final String REASON_LIVE_EXCLUSIVE = "LIVE_EXCLUSIVE";
    private static final String REASON_UNAVAILABLE = "UNAVAILABLE";
    private static final String FAILURE_ALL_ITEMS_UNAVAILABLE = "ALL_ITEMS_UNAVAILABLE";
    private static final String FAILURE_RESERVATION_FAILED = "RESERVATION_FAILED";
    private static final String FAILURE_STORE_CHECKOUT_ERROR = "STORE_CHECKOUT_ERROR";

    private final CartStorePort cartStorePort;
    private final LoadCartProductInfoPort loadCartProductInfoPort;
    private final PlaceCartOrderPort placeCartOrderPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public CheckoutCartResponse checkout(CheckoutCartCommand command) {
        Map<UUID, List<SelectedItem>> byStore = command.selectedItems().stream()
                .collect(Collectors.groupingBy(SelectedItem::storeId, LinkedHashMap::new, Collectors.toList()));

        List<StoreCheckoutResult> results = new ArrayList<>();
        for (var entry : byStore.entrySet()) {
            results.add(checkoutStore(command.buyerId(), entry.getKey(), entry.getValue()));
        }
        return new CheckoutCartResponse(results);
    }

    /**
     * Outer isolation boundary for a single store's checkout (JDA2-002):
     * catches ANY exception from the whole per-store flow — including
     * {@code cartStorePort.load(...)} and {@code loadCartProductInfoPort.loadForCart(...)},
     * which are not guarded by {@link #doCheckoutStore}'s own inner
     * try/catch — so one store's infrastructure failure never propagates out
     * of {@link #checkout} and discards already-computed sibling results
     * (design D8, best-effort per store).
     */
    private StoreCheckoutResult checkoutStore(UUID buyerId, UUID storeId, List<SelectedItem> items) {
        try {
            StoreCheckoutResult result = doCheckoutStore(buyerId, storeId, items);
            publishCheckoutCompleted(buyerId, result);
            return result;
        } catch (Exception e) {
            log.error("Unexpected error during checkout for store {}: {}", storeId, e.getMessage(), e);
            StoreCheckoutResult result = new StoreCheckoutResult(
                    storeId, false, null, null, null, List.of(), FAILURE_STORE_CHECKOUT_ERROR);
            publishCheckoutCompleted(buyerId, result);
            return result;
        }
    }

    private void publishCheckoutCompleted(UUID buyerId, StoreCheckoutResult result) {
        eventPublisher.publishEvent(new CartCheckoutCompletedEvent(
                buyerId, result.storeId(), result.succeeded(), result.orderId(),
                result.total(), result.failureReason()));
    }

    private StoreCheckoutResult doCheckoutStore(UUID buyerId, UUID storeId, List<SelectedItem> items) {
        Cart cart = cartStorePort.load(buyerId, storeId);

        // JDA2-004: coalesce duplicate (productId,variantId) selections for
        // this store before the stock check and before building order lines,
        // so a duplicated selection can't produce two independent stock
        // checks / two order lines against the same cart-stored quantity.
        // The cart-stored quantity (cartLine.quantity()) is the sole source
        // of truth for how many units are being ordered — repeating a
        // selection must never multiply or sum it.
        Set<CartLineKey> selectedKeys = new LinkedHashSet<>();
        for (SelectedItem item : items) {
            selectedKeys.add(new CartLineKey(item.productId(), item.variantId()));
        }

        Set<CartLineRef> refs = selectedKeys.stream()
                .map(key -> new CartLineRef(key.productId(), key.variantId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<CartLineRef, CartProductInfo> infoByRef = loadCartProductInfoPort.loadForCart(refs);

        List<CartOrderLine> orderLines = new ArrayList<>();
        List<SkippedLine> skipped = new ArrayList<>();
        String currency = null;

        for (CartLineKey key : selectedKeys) {
            UUID productId = key.productId();
            UUID variantId = key.variantId();

            var cartLine = cart.findLine(key);
            if (cartLine.isEmpty()) {
                skipped.add(new SkippedLine(productId, variantId, REASON_UNAVAILABLE));
                continue;
            }

            var ref = new CartLineRef(productId, variantId);
            var info = infoByRef.get(ref);
            if (info == null) {
                skipped.add(new SkippedLine(productId, variantId, REASON_UNAVAILABLE));
                continue;
            }
            if (info.exclusiveToActiveLive()) {
                skipped.add(new SkippedLine(productId, variantId, REASON_LIVE_EXCLUSIVE));
                continue;
            }
            if (!info.active() || info.paused()) {
                skipped.add(new SkippedLine(productId, variantId, REASON_UNAVAILABLE));
                continue;
            }
            int quantity = cartLine.get().quantity();
            if (quantity > info.availableStock()) {
                skipped.add(new SkippedLine(productId, variantId, REASON_OUT_OF_STOCK));
                continue;
            }

            currency = info.currency();
            orderLines.add(new CartOrderLine(productId, variantId, info.name(), info.unitPrice(), quantity));
        }

        if (orderLines.isEmpty()) {
            return new StoreCheckoutResult(storeId, false, null, null, null, skipped, FAILURE_ALL_ITEMS_UNAVAILABLE);
        }

        try {
            var placed = placeCartOrderPort.placeOrder(new PlaceCartOrderCommand(buyerId, storeId, currency, orderLines));

            for (CartOrderLine line : orderLines) {
                cartStorePort.removeLine(buyerId, storeId, line.productId(), line.variantId());
            }

            return new StoreCheckoutResult(storeId, true, placed.orderId(), placed.total(), placed.currency(),
                    skipped, null);
        } catch (Exception e) {
            log.warn("Checkout failed for store {}: {}", storeId, e.getMessage());
            List<SkippedLine> allSkipped = new ArrayList<>(skipped);
            for (CartOrderLine line : orderLines) {
                allSkipped.add(new SkippedLine(line.productId(), line.variantId(), REASON_OUT_OF_STOCK));
            }
            return new StoreCheckoutResult(storeId, false, null, null, null, allSkipped, FAILURE_RESERVATION_FAILED);
        }
    }
}
