package com.livecomerce.cart.application;

import com.livecomerce.cart.application.port.in.AddToCartUseCase.AddToCartCommand;
import com.livecomerce.cart.application.port.in.ChangeQuantityUseCase.ChangeQuantityCommand;
import com.livecomerce.cart.application.port.in.RemoveFromCartUseCase.RemoveFromCartCommand;
import com.livecomerce.cart.application.port.out.CartStorePort;
import com.livecomerce.cart.domain.Cart;
import com.livecomerce.cart.domain.CartItem;
import com.livecomerce.cart.domain.CartLineKey;
import com.livecomerce.catalog.LoadCartProductInfoPort;
import com.livecomerce.catalog.LoadCartProductInfoPort.CartLineRef;
import com.livecomerce.catalog.LoadCartProductInfoPort.CartProductInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock CartStorePort cartStorePort;
    @Mock LoadCartProductInfoPort loadCartProductInfoPort;

    CartService sut;

    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID VARIANT_ID = UUID.randomUUID();

    private static CartProductInfo info(int availableStock, boolean exclusive, boolean active) {
        return new CartProductInfo(PRODUCT_ID, VARIANT_ID, STORE_ID, "Playera", "http://img",
                new BigDecimal("199.00"), "MXN", availableStock, active, false, exclusive);
    }

    private void setUp() {
        sut = new CartService(cartStorePort, loadCartProductInfoPort);
    }

    // --- addToCart (4.2) ---

    @Test
    void addToCart_newLine_delegatesToAddOrIncrement() {
        setUp();
        var ref = new CartLineRef(PRODUCT_ID, VARIANT_ID);
        when(loadCartProductInfoPort.loadForCart(Set.of(ref))).thenReturn(Map.of(ref, info(10, false, true)));

        var result = sut.addToCart(new AddToCartCommand(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 2));

        assertThat(result.success()).isTrue();
        verify(cartStorePort).addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 2);
    }

    @Test
    void addToCart_existingLine_alsoDelegatesToAddOrIncrement_soAdapterCanMergeInsteadOfDuplicate() {
        setUp();
        var ref = new CartLineRef(PRODUCT_ID, VARIANT_ID);
        when(loadCartProductInfoPort.loadForCart(Set.of(ref))).thenReturn(Map.of(ref, info(10, false, true)));

        sut.addToCart(new AddToCartCommand(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 2));
        sut.addToCart(new AddToCartCommand(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 1));

        // CartService always calls the single merge-or-create op; the "no
        // duplicate line" invariant itself is the storage adapter's HINCRBY
        // contract, covered by RedisCartStoreAdapterTest/InMemoryCartStoreAdapterTest.
        verify(cartStorePort).addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 2);
        verify(cartStorePort).addOrIncrement(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 1);
    }

    @Test
    void addToCart_liveExclusiveProduct_rejectedWithoutTouchingStorage() {
        setUp();
        var ref = new CartLineRef(PRODUCT_ID, VARIANT_ID);
        when(loadCartProductInfoPort.loadForCart(Set.of(ref))).thenReturn(Map.of(ref, info(10, true, true)));

        var result = sut.addToCart(new AddToCartCommand(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 1));

        assertThat(result.success()).isFalse();
        assertThat(result.rejectionReason()).isEqualTo("LIVE_EXCLUSIVE");
        verify(cartStorePort, never()).addOrIncrement(any(), any(), any(), any(), anyInt());
    }

    @Test
    void addToCart_catalogLookupFails_rejectedUnavailable_perD6() {
        setUp();
        var ref = new CartLineRef(PRODUCT_ID, VARIANT_ID);
        when(loadCartProductInfoPort.loadForCart(Set.of(ref))).thenReturn(Map.of());

        var result = sut.addToCart(new AddToCartCommand(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 1));

        assertThat(result.success()).isFalse();
        assertThat(result.rejectionReason()).isEqualTo("UNAVAILABLE");
        verify(cartStorePort, never()).addOrIncrement(any(), any(), any(), any(), anyInt());
    }

    // --- changeQuantity (4.3) ---

    @Test
    void changeQuantity_incrementHappyPath_delegatesDelta() {
        setUp();
        var key = new CartLineKey(PRODUCT_ID, VARIANT_ID);
        var cart = Cart.of(BUYER_ID, STORE_ID, List.of(CartItem.of(key, 2)));
        when(cartStorePort.load(BUYER_ID, STORE_ID)).thenReturn(cart);
        var ref = new CartLineRef(PRODUCT_ID, VARIANT_ID);
        when(loadCartProductInfoPort.loadForCart(Set.of(ref))).thenReturn(Map.of(ref, info(10, false, true)));
        when(cartStorePort.changeQuantity(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 1)).thenReturn(3);

        var result = sut.changeQuantity(new ChangeQuantityCommand(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 1));

        assertThat(result.success()).isTrue();
        assertThat(result.resultingQuantity()).isEqualTo(3);
    }

    @Test
    void changeQuantity_incrementBeyondAvailableStock_blocksEntirely_noPartialApply() {
        setUp();
        var key = new CartLineKey(PRODUCT_ID, VARIANT_ID);
        var cart = Cart.of(BUYER_ID, STORE_ID, List.of(CartItem.of(key, 8)));
        when(cartStorePort.load(BUYER_ID, STORE_ID)).thenReturn(cart);
        var ref = new CartLineRef(PRODUCT_ID, VARIANT_ID);
        when(loadCartProductInfoPort.loadForCart(Set.of(ref))).thenReturn(Map.of(ref, info(8, false, true)));

        var result = sut.changeQuantity(new ChangeQuantityCommand(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 1));

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(result.availableStock()).isEqualTo(8);
        verify(cartStorePort, never()).changeQuantity(any(), any(), any(), any(), anyInt());
    }

    @Test
    void changeQuantity_decrementHappyPath_delegatesNegativeDeltaWithoutStockCheck() {
        setUp();
        var key = new CartLineKey(PRODUCT_ID, VARIANT_ID);
        var cart = Cart.of(BUYER_ID, STORE_ID, List.of(CartItem.of(key, 3)));
        when(cartStorePort.load(BUYER_ID, STORE_ID)).thenReturn(cart);
        when(cartStorePort.changeQuantity(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, -1)).thenReturn(2);

        var result = sut.changeQuantity(new ChangeQuantityCommand(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, -1));

        assertThat(result.success()).isTrue();
        assertThat(result.resultingQuantity()).isEqualTo(2);
        verify(loadCartProductInfoPort, never()).loadForCart(any());
    }

    @Test
    void changeQuantity_decrementToZero_removesLine() {
        setUp();
        var key = new CartLineKey(PRODUCT_ID, VARIANT_ID);
        var cart = Cart.of(BUYER_ID, STORE_ID, List.of(CartItem.of(key, 1)));
        when(cartStorePort.load(BUYER_ID, STORE_ID)).thenReturn(cart);
        when(cartStorePort.changeQuantity(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, -1)).thenReturn(0);

        var result = sut.changeQuantity(new ChangeQuantityCommand(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, -1));

        assertThat(result.success()).isTrue();
        assertThat(result.resultingQuantity()).isZero();
    }

    @Test
    void changeQuantity_lineNotFound_returnsFailureWithoutCallingCatalog() {
        setUp();
        var cart = Cart.empty(BUYER_ID, STORE_ID);
        when(cartStorePort.load(BUYER_ID, STORE_ID)).thenReturn(cart);

        var result = sut.changeQuantity(new ChangeQuantityCommand(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID, 1));

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).isEqualTo("LINE_NOT_FOUND");
        verify(loadCartProductInfoPort, never()).loadForCart(any());
    }

    // --- removeLine (4.4) ---

    @Test
    void removeLine_happyPath_delegates() {
        setUp();
        sut.removeLine(new RemoveFromCartCommand(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID));

        verify(cartStorePort).removeLine(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID);
    }

    @Test
    void removeLine_idempotentNoOpOnAbsentItem_stillSucceeds() {
        setUp();
        // storePort.removeLine is documented idempotent; CartService just delegates
        sut.removeLine(new RemoveFromCartCommand(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID));
        sut.removeLine(new RemoveFromCartCommand(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID));

        verify(cartStorePort, times(2)).removeLine(BUYER_ID, STORE_ID, PRODUCT_ID, VARIANT_ID);
    }

    // --- combined view (4.5, 4.6) ---

    @Test
    void combinedView_multiStoreGrouping() {
        setUp();
        var storeA = UUID.randomUUID();
        var storeB = UUID.randomUUID();
        var productA = UUID.randomUUID();
        var productB = UUID.randomUUID();
        when(cartStorePort.loadStoreIds(BUYER_ID)).thenReturn(Set.of(storeA, storeB));
        var cartA = Cart.of(BUYER_ID, storeA, List.of(CartItem.of(new CartLineKey(productA, null), 1)));
        var cartB = Cart.of(BUYER_ID, storeB, List.of(CartItem.of(new CartLineKey(productB, null), 2)));
        when(cartStorePort.load(BUYER_ID, storeA)).thenReturn(cartA);
        when(cartStorePort.load(BUYER_ID, storeB)).thenReturn(cartB);
        var refA = new CartLineRef(productA, null);
        var refB = new CartLineRef(productB, null);
        var infoA = new CartProductInfo(productA, null, storeA, "A", null, BigDecimal.TEN, "MXN", 5, true, false, false);
        var infoB = new CartProductInfo(productB, null, storeB, "B", null, BigDecimal.ONE, "MXN", 5, true, false, false);
        when(loadCartProductInfoPort.loadForCart(any())).thenReturn(Map.of(refA, infoA, refB, infoB));

        var view = sut.getCombinedView(BUYER_ID);

        assertThat(view.stores()).hasSize(2);
        assertThat(view.stores())
                .extracting(com.livecomerce.cart.application.port.in.GetCombinedCartViewUseCase.StoreCartView::storeId)
                .containsExactlyInAnyOrder(storeA, storeB);
    }

    @Test
    void combinedView_emptyState_returnsEmptyListNotError() {
        setUp();
        when(cartStorePort.loadStoreIds(BUYER_ID)).thenReturn(Set.of());

        var view = sut.getCombinedView(BUYER_ID);

        assertThat(view.stores()).isEmpty();
    }

    @Test
    void combinedView_deactivatedProduct_excludedFromView_dataUntouched() {
        setUp();
        when(cartStorePort.loadStoreIds(BUYER_ID)).thenReturn(Set.of(STORE_ID));
        var cart = Cart.of(BUYER_ID, STORE_ID, List.of(CartItem.of(new CartLineKey(PRODUCT_ID, VARIANT_ID), 1)));
        when(cartStorePort.load(BUYER_ID, STORE_ID)).thenReturn(cart);
        var ref = new CartLineRef(PRODUCT_ID, VARIANT_ID);
        when(loadCartProductInfoPort.loadForCart(any())).thenReturn(Map.of(ref, info(5, false, false)));

        var view = sut.getCombinedView(BUYER_ID);

        assertThat(view.stores()).isEmpty();
        verify(cartStorePort, never()).removeLine(any(), any(), any(), any());
        verify(cartStorePort, never()).clear(any(), any());
    }

    @Test
    void combinedView_preExistingLineBecomesLiveExclusive_staysVisibleButFlagged() {
        setUp();
        when(cartStorePort.loadStoreIds(BUYER_ID)).thenReturn(Set.of(STORE_ID));
        var cart = Cart.of(BUYER_ID, STORE_ID, List.of(CartItem.of(new CartLineKey(PRODUCT_ID, VARIANT_ID), 1)));
        when(cartStorePort.load(BUYER_ID, STORE_ID)).thenReturn(cart);
        var ref = new CartLineRef(PRODUCT_ID, VARIANT_ID);
        when(loadCartProductInfoPort.loadForCart(any())).thenReturn(Map.of(ref, info(5, true, true)));

        var view = sut.getCombinedView(BUYER_ID);

        assertThat(view.stores()).hasSize(1);
        var line = view.stores().get(0).lines().get(0);
        assertThat(line.blockedReason()).isEqualTo("LIVE_EXCLUSIVE");
    }

    @Test
    void combinedView_d6_oneLineLookupFailure_excludesOnlyThatLine_siblingsUnaffected() {
        setUp();
        var productOk = UUID.randomUUID();
        var productFailed = UUID.randomUUID();
        when(cartStorePort.loadStoreIds(BUYER_ID)).thenReturn(Set.of(STORE_ID));
        var cart = Cart.of(BUYER_ID, STORE_ID, List.of(
                CartItem.of(new CartLineKey(productOk, null), 1),
                CartItem.of(new CartLineKey(productFailed, null), 1)));
        when(cartStorePort.load(BUYER_ID, STORE_ID)).thenReturn(cart);
        var refOk = new CartLineRef(productOk, null);
        var infoOk = new CartProductInfo(productOk, null, STORE_ID, "Ok", null, BigDecimal.TEN, "MXN", 5, true, false, false);
        // productFailed simply absent from the map — D6 fail-closed sentinel
        when(loadCartProductInfoPort.loadForCart(any())).thenReturn(Map.of(refOk, infoOk));

        var view = sut.getCombinedView(BUYER_ID);

        assertThat(view.stores()).hasSize(1);
        assertThat(view.stores().get(0).lines()).hasSize(1);
        assertThat(view.stores().get(0).lines().get(0).name()).isEqualTo("Ok");
    }

}
