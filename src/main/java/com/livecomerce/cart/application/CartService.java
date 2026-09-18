package com.livecomerce.cart.application;

import com.livecomerce.cart.application.port.in.AddToCartUseCase;
import com.livecomerce.cart.application.port.in.ChangeQuantityUseCase;
import com.livecomerce.cart.application.port.in.GetCombinedCartViewUseCase;
import com.livecomerce.cart.application.port.in.RemoveFromCartUseCase;
import com.livecomerce.cart.application.port.out.CartStorePort;
import com.livecomerce.cart.domain.Cart;
import com.livecomerce.cart.domain.CartItem;
import com.livecomerce.cart.domain.CartLineKey;
import com.livecomerce.catalog.LoadCartProductInfoPort;
import com.livecomerce.catalog.LoadCartProductInfoPort.CartLineRef;
import com.livecomerce.catalog.LoadCartProductInfoPort.CartProductInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Add / change-quantity / remove / combined-view for the storefront cart.
 * Every catalog-hydration touch point goes through {@link
 * LoadCartProductInfoPort}, whose D6 fail-closed contract this class must
 * faithfully propagate: a missing {@link CartProductInfo} for a line means
 * that line is unavailable, never silently treated as available or
 * non-exclusive.
 */
@Service
@RequiredArgsConstructor
public class CartService implements AddToCartUseCase, ChangeQuantityUseCase, RemoveFromCartUseCase,
        GetCombinedCartViewUseCase {

    private static final String REASON_LIVE_EXCLUSIVE = "LIVE_EXCLUSIVE";
    private static final String REASON_UNAVAILABLE = "UNAVAILABLE";
    private static final String REASON_INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";
    private static final String REASON_LINE_NOT_FOUND = "LINE_NOT_FOUND";
    private static final String REASON_QUANTITY_LIMIT_EXCEEDED = "QUANTITY_LIMIT_EXCEEDED";

    private final CartStorePort cartStorePort;
    private final LoadCartProductInfoPort loadCartProductInfoPort;

    @Override
    public AddToCartResult addToCart(AddToCartCommand command) {
        var ref = new CartLineRef(command.productId(), command.variantId());
        var info = loadCartProductInfoPort.loadForCart(Set.of(ref)).get(ref);

        if (info == null) {
            return AddToCartResult.rejected(REASON_UNAVAILABLE);
        }
        if (info.exclusiveToActiveLive()) {
            return AddToCartResult.rejected(REASON_LIVE_EXCLUSIVE);
        }
        if (!info.active() || info.paused()) {
            return AddToCartResult.rejected(REASON_UNAVAILABLE);
        }

        var cart = cartStorePort.load(command.buyerId(), command.storeId());
        var key = new CartLineKey(command.productId(), command.variantId());
        int currentQuantity = cart.findLine(key).map(CartItem::quantity).orElse(0);
        int prospectiveQuantity;
        try {
            prospectiveQuantity = Math.addExact(currentQuantity, command.quantity());
        } catch (ArithmeticException overflow) {
            // Overflow can only mean the requested quantity, combined with
            // what's already in the cart, is nonsensically large — treat it
            // the same as any other over-cap request rather than letting a
            // wrapped-around (possibly negative) value slip past the check
            // below and get persisted.
            return AddToCartResult.rejected(REASON_QUANTITY_LIMIT_EXCEEDED);
        }
        if (prospectiveQuantity > CartItem.MAX_QUANTITY) {
            return AddToCartResult.rejected(REASON_QUANTITY_LIMIT_EXCEEDED);
        }

        cartStorePort.addOrIncrement(command.buyerId(), command.storeId(), command.productId(),
                command.variantId(), command.quantity());
        return AddToCartResult.accepted();
    }

    @Override
    public ChangeQuantityResult changeQuantity(ChangeQuantityCommand command) {
        var cart = cartStorePort.load(command.buyerId(), command.storeId());
        var key = new CartLineKey(command.productId(), command.variantId());
        var line = cart.findLine(key);

        if (line.isEmpty()) {
            return ChangeQuantityResult.failure(REASON_LINE_NOT_FOUND);
        }

        Integer availableStockBound = null;
        if (command.delta() > 0) {
            var ref = new CartLineRef(command.productId(), command.variantId());
            var info = loadCartProductInfoPort.loadForCart(Set.of(ref)).get(ref);
            if (info == null) {
                return ChangeQuantityResult.failure(REASON_UNAVAILABLE);
            }
            if (!info.active() || info.paused()) {
                return ChangeQuantityResult.failure(REASON_UNAVAILABLE);
            }
            int newQuantity;
            try {
                newQuantity = Math.addExact(line.get().quantity(), command.delta());
            } catch (ArithmeticException overflow) {
                // Same overflow-as-over-cap treatment as addToCart above.
                return ChangeQuantityResult.quantityLimitExceeded();
            }
            if (newQuantity > CartItem.MAX_QUANTITY) {
                return ChangeQuantityResult.quantityLimitExceeded();
            }
            if (newQuantity > info.availableStock()) {
                return ChangeQuantityResult.insufficientStock(info.availableStock());
            }
            availableStockBound = info.availableStock();
        }

        int resulting = cartStorePort.changeQuantity(command.buyerId(), command.storeId(),
                command.productId(), command.variantId(), command.delta(), availableStockBound);
        return ChangeQuantityResult.success(resulting);
    }

    @Override
    public void removeLine(RemoveFromCartCommand command) {
        cartStorePort.removeLine(command.buyerId(), command.storeId(), command.productId(), command.variantId());
    }

    @Override
    public CombinedCartView getCombinedView(UUID buyerId) {
        Set<UUID> storeIds = cartStorePort.loadStoreIds(buyerId);
        if (storeIds.isEmpty()) {
            return new CombinedCartView(List.of());
        }

        List<Cart> carts = new ArrayList<>();
        Set<CartLineRef> allRefs = new LinkedHashSet<>();
        for (UUID storeId : storeIds) {
            Cart cart = cartStorePort.load(buyerId, storeId);
            if (cart.isEmpty()) {
                continue;
            }
            carts.add(cart);
            for (CartItem item : cart.items()) {
                allRefs.add(new CartLineRef(item.key().productId(), item.key().variantId()));
            }
        }

        Map<CartLineRef, CartProductInfo> infoByRef = loadCartProductInfoPort.loadForCart(allRefs);

        List<StoreCartView> storeViews = new ArrayList<>();
        for (Cart cart : carts) {
            List<CartLineView> lineViews = new ArrayList<>();
            for (CartItem item : cart.items()) {
                var ref = new CartLineRef(item.key().productId(), item.key().variantId());
                var info = infoByRef.get(ref);
                if (info == null || !info.active() || info.paused()) {
                    // D6 fail-closed lookup failure, a deactivated
                    // product/store (see catalog.application.StoreEventListener),
                    // or a seller-paused product: exclude the line from the
                    // view without touching storage.
                    continue;
                }
                String blockedReason = info.exclusiveToActiveLive() ? REASON_LIVE_EXCLUSIVE : null;
                lineViews.add(new CartLineView(
                        item.key().productId(),
                        item.key().variantId(),
                        info.name(),
                        info.imageUrl(),
                        info.unitPrice(),
                        info.currency(),
                        item.quantity(),
                        info.availableStock(),
                        blockedReason));
            }
            if (!lineViews.isEmpty()) {
                storeViews.add(new StoreCartView(cart.storeId(), lineViews));
            }
        }

        return new CombinedCartView(storeViews);
    }
}
