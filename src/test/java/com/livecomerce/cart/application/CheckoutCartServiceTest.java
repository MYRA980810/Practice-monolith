package com.livecomerce.cart.application;

import com.livecomerce.cart.CartCheckoutCompletedEvent;
import com.livecomerce.cart.application.port.in.CheckoutCartUseCase.CheckoutCartCommand;
import com.livecomerce.cart.application.port.in.CheckoutCartUseCase.SelectedItem;
import com.livecomerce.cart.application.port.out.CartStorePort;
import com.livecomerce.cart.domain.Cart;
import com.livecomerce.cart.domain.CartItem;
import com.livecomerce.cart.domain.CartLineKey;
import com.livecomerce.catalog.LoadCartProductInfoPort;
import com.livecomerce.catalog.LoadCartProductInfoPort.CartLineRef;
import com.livecomerce.catalog.LoadCartProductInfoPort.CartProductInfo;
import com.livecomerce.order.PlaceCartOrderPort;
import com.livecomerce.order.PlaceCartOrderPort.PlaceCartOrderCommand;
import com.livecomerce.order.PlaceCartOrderPort.PlacedOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutCartServiceTest {

    @Mock CartStorePort cartStorePort;
    @Mock LoadCartProductInfoPort loadCartProductInfoPort;
    @Mock PlaceCartOrderPort placeCartOrderPort;
    @Mock ApplicationEventPublisher eventPublisher;

    CheckoutCartService sut;

    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID STORE_A = UUID.randomUUID();
    private static final UUID STORE_B = UUID.randomUUID();

    private void setUp() {
        sut = new CheckoutCartService(cartStorePort, loadCartProductInfoPort, placeCartOrderPort, eventPublisher);
    }

    private static CartProductInfo info(UUID productId, UUID storeId, int availableStock, boolean exclusive) {
        return new CartProductInfo(productId, null, storeId, "Product-" + productId, null,
                new BigDecimal("10.00"), "MXN", availableStock, true, false, exclusive);
    }

    @Test
    void checkout_singleStore_multipleItems_placesOneOrderWithAllItems() {
        setUp();
        var p1 = UUID.randomUUID();
        var p2 = UUID.randomUUID();
        var cart = Cart.of(BUYER_ID, STORE_A, List.of(
                CartItem.of(new CartLineKey(p1, null), 2),
                CartItem.of(new CartLineKey(p2, null), 1)));
        when(cartStorePort.load(BUYER_ID, STORE_A)).thenReturn(cart);
        when(loadCartProductInfoPort.loadForCart(any())).thenReturn(Map.of(
                new CartLineRef(p1, null), info(p1, STORE_A, 10, false),
                new CartLineRef(p2, null), info(p2, STORE_A, 10, false)));
        var orderId = UUID.randomUUID();
        when(placeCartOrderPort.placeOrder(any())).thenReturn(new PlacedOrder(orderId, new BigDecimal("30.00"), "MXN"));

        var response = sut.checkout(new CheckoutCartCommand(BUYER_ID, List.of(
                new SelectedItem(STORE_A, p1, null),
                new SelectedItem(STORE_A, p2, null))));

        assertThat(response.results()).hasSize(1);
        var result = response.results().get(0);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.skippedLines()).isEmpty();

        ArgumentCaptor<PlaceCartOrderCommand> captor = ArgumentCaptor.forClass(PlaceCartOrderCommand.class);
        verify(placeCartOrderPort, times(1)).placeOrder(captor.capture());
        assertThat(captor.getValue().lines()).hasSize(2);

        verify(cartStorePort).removeLine(BUYER_ID, STORE_A, p1, null);
        verify(cartStorePort).removeLine(BUYER_ID, STORE_A, p2, null);
    }

    @Test
    void checkout_multiStore_bestEffort_storeASucceedsRegardlessOfStoreBFailure() {
        setUp();
        var pA = UUID.randomUUID();
        var pB = UUID.randomUUID();
        var cartA = Cart.of(BUYER_ID, STORE_A, List.of(CartItem.of(new CartLineKey(pA, null), 1)));
        var cartB = Cart.of(BUYER_ID, STORE_B, List.of(CartItem.of(new CartLineKey(pB, null), 5)));
        when(cartStorePort.load(BUYER_ID, STORE_A)).thenReturn(cartA);
        when(cartStorePort.load(BUYER_ID, STORE_B)).thenReturn(cartB);
        when(loadCartProductInfoPort.loadForCart(any())).thenReturn(Map.of(
                new CartLineRef(pA, null), info(pA, STORE_A, 10, false),
                new CartLineRef(pB, null), info(pB, STORE_B, 2, false))); // pB out of stock: wants 5, only 2 available
        var orderId = UUID.randomUUID();
        when(placeCartOrderPort.placeOrder(any())).thenReturn(new PlacedOrder(orderId, BigDecimal.TEN, "MXN"));

        var response = sut.checkout(new CheckoutCartCommand(BUYER_ID, List.of(
                new SelectedItem(STORE_A, pA, null),
                new SelectedItem(STORE_B, pB, null))));

        assertThat(response.results()).hasSize(2);
        var resultA = response.results().stream().filter(r -> r.storeId().equals(STORE_A)).findFirst().orElseThrow();
        var resultB = response.results().stream().filter(r -> r.storeId().equals(STORE_B)).findFirst().orElseThrow();

        assertThat(resultA.succeeded()).isTrue();
        assertThat(resultA.orderId()).isEqualTo(orderId);

        assertThat(resultB.succeeded()).isFalse();
        assertThat(resultB.failureReason()).isEqualTo("ALL_ITEMS_UNAVAILABLE");
        assertThat(resultB.skippedLines()).containsExactly(
                new com.livecomerce.cart.application.port.in.CheckoutCartUseCase.SkippedLine(pB, null, "OUT_OF_STOCK"));

        verify(placeCartOrderPort, times(1)).placeOrder(any());
        verify(cartStorePort, never()).removeLine(BUYER_ID, STORE_B, pB, null);
    }

    @Test
    void checkout_oneLineOutOfStock_skipsThatLine_restOfStoreProceeds() {
        setUp();
        var p1 = UUID.randomUUID();
        var p2 = UUID.randomUUID();
        var cart = Cart.of(BUYER_ID, STORE_A, List.of(
                CartItem.of(new CartLineKey(p1, null), 4),
                CartItem.of(new CartLineKey(p2, null), 1)));
        when(cartStorePort.load(BUYER_ID, STORE_A)).thenReturn(cart);
        when(loadCartProductInfoPort.loadForCart(any())).thenReturn(Map.of(
                new CartLineRef(p1, null), info(p1, STORE_A, 1, false), // wants 4, only 1 available
                new CartLineRef(p2, null), info(p2, STORE_A, 10, false)));
        var orderId = UUID.randomUUID();
        when(placeCartOrderPort.placeOrder(any())).thenReturn(new PlacedOrder(orderId, BigDecimal.TEN, "MXN"));

        var response = sut.checkout(new CheckoutCartCommand(BUYER_ID, List.of(
                new SelectedItem(STORE_A, p1, null),
                new SelectedItem(STORE_A, p2, null))));

        var result = response.results().get(0);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.skippedLines()).containsExactly(
                new com.livecomerce.cart.application.port.in.CheckoutCartUseCase.SkippedLine(p1, null, "OUT_OF_STOCK"));

        ArgumentCaptor<PlaceCartOrderCommand> captor = ArgumentCaptor.forClass(PlaceCartOrderCommand.class);
        verify(placeCartOrderPort).placeOrder(captor.capture());
        assertThat(captor.getValue().lines()).hasSize(1);
        assertThat(captor.getValue().lines().get(0).productId()).isEqualTo(p2);

        verify(cartStorePort).removeLine(BUYER_ID, STORE_A, p2, null);
        verify(cartStorePort, never()).removeLine(BUYER_ID, STORE_A, p1, null);
    }

    @Test
    void checkout_lineBecameLiveExclusive_skippedOnlyAtCheckout_restProceeds() {
        setUp();
        var p1 = UUID.randomUUID();
        var p2 = UUID.randomUUID();
        var cart = Cart.of(BUYER_ID, STORE_A, List.of(
                CartItem.of(new CartLineKey(p1, null), 1),
                CartItem.of(new CartLineKey(p2, null), 1)));
        when(cartStorePort.load(BUYER_ID, STORE_A)).thenReturn(cart);
        when(loadCartProductInfoPort.loadForCart(any())).thenReturn(Map.of(
                new CartLineRef(p1, null), info(p1, STORE_A, 10, true), // now live-exclusive
                new CartLineRef(p2, null), info(p2, STORE_A, 10, false)));
        var orderId = UUID.randomUUID();
        when(placeCartOrderPort.placeOrder(any())).thenReturn(new PlacedOrder(orderId, BigDecimal.TEN, "MXN"));

        var response = sut.checkout(new CheckoutCartCommand(BUYER_ID, List.of(
                new SelectedItem(STORE_A, p1, null),
                new SelectedItem(STORE_A, p2, null))));

        var result = response.results().get(0);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.skippedLines()).containsExactly(
                new com.livecomerce.cart.application.port.in.CheckoutCartUseCase.SkippedLine(p1, null, "LIVE_EXCLUSIVE"));
    }

    @Test
    void checkout_d6_catalogLookupFailedLine_skippedAsUnavailable_notFailingWholeStore() {
        setUp();
        var p1 = UUID.randomUUID();
        var p2 = UUID.randomUUID();
        var cart = Cart.of(BUYER_ID, STORE_A, List.of(
                CartItem.of(new CartLineKey(p1, null), 1),
                CartItem.of(new CartLineKey(p2, null), 1)));
        when(cartStorePort.load(BUYER_ID, STORE_A)).thenReturn(cart);
        // p1 absent from the map entirely — D6 fail-closed sentinel
        when(loadCartProductInfoPort.loadForCart(any())).thenReturn(Map.of(
                new CartLineRef(p2, null), info(p2, STORE_A, 10, false)));
        var orderId = UUID.randomUUID();
        when(placeCartOrderPort.placeOrder(any())).thenReturn(new PlacedOrder(orderId, BigDecimal.TEN, "MXN"));

        var response = sut.checkout(new CheckoutCartCommand(BUYER_ID, List.of(
                new SelectedItem(STORE_A, p1, null),
                new SelectedItem(STORE_A, p2, null))));

        var result = response.results().get(0);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.skippedLines()).containsExactly(
                new com.livecomerce.cart.application.port.in.CheckoutCartUseCase.SkippedLine(p1, null, "UNAVAILABLE"));
    }

    @Test
    void checkout_linePausedOrDeactivated_skippedAsUnavailable() {
        setUp();
        var p1 = UUID.randomUUID();
        var cart = Cart.of(BUYER_ID, STORE_A, List.of(CartItem.of(new CartLineKey(p1, null), 1)));
        when(cartStorePort.load(BUYER_ID, STORE_A)).thenReturn(cart);
        var pausedInfo = new CartProductInfo(p1, null, STORE_A, "Product-" + p1, null,
                new BigDecimal("10.00"), "MXN", 10, true, true, false);
        when(loadCartProductInfoPort.loadForCart(any())).thenReturn(Map.of(new CartLineRef(p1, null), pausedInfo));

        var response = sut.checkout(new CheckoutCartCommand(BUYER_ID, List.of(new SelectedItem(STORE_A, p1, null))));

        var result = response.results().get(0);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.skippedLines()).containsExactly(
                new com.livecomerce.cart.application.port.in.CheckoutCartUseCase.SkippedLine(p1, null, "UNAVAILABLE"));
    }

    @Test
    void checkout_duplicateSelectedItem_dedupedIntoSingleOrderLineWithCartQuantity() {
        setUp();
        var p1 = UUID.randomUUID();
        var cart = Cart.of(BUYER_ID, STORE_A, List.of(CartItem.of(new CartLineKey(p1, null), 2)));
        when(cartStorePort.load(BUYER_ID, STORE_A)).thenReturn(cart);
        when(loadCartProductInfoPort.loadForCart(any()))
                .thenReturn(Map.of(new CartLineRef(p1, null), info(p1, STORE_A, 10, false)));
        var orderId = UUID.randomUUID();
        when(placeCartOrderPort.placeOrder(any())).thenReturn(new PlacedOrder(orderId, BigDecimal.TEN, "MXN"));

        var response = sut.checkout(new CheckoutCartCommand(BUYER_ID, List.of(
                new SelectedItem(STORE_A, p1, null),
                new SelectedItem(STORE_A, p1, null))));

        var result = response.results().get(0);
        assertThat(result.succeeded()).isTrue();

        ArgumentCaptor<PlaceCartOrderCommand> captor = ArgumentCaptor.forClass(PlaceCartOrderCommand.class);
        verify(placeCartOrderPort, times(1)).placeOrder(captor.capture());
        assertThat(captor.getValue().lines()).hasSize(1);
        assertThat(captor.getValue().lines().get(0).quantity()).isEqualTo(2);

        verify(cartStorePort, times(1)).removeLine(BUYER_ID, STORE_A, p1, null);
    }

    @Test
    void checkout_selectedItemRepeatedManyTimes_stillOrdersOnlyCartQuantity() {
        setUp();
        var p1 = UUID.randomUUID();
        var cart = Cart.of(BUYER_ID, STORE_A, List.of(CartItem.of(new CartLineKey(p1, null), 5)));
        when(cartStorePort.load(BUYER_ID, STORE_A)).thenReturn(cart);
        when(loadCartProductInfoPort.loadForCart(any()))
                .thenReturn(Map.of(new CartLineRef(p1, null), info(p1, STORE_A, 10, false)));
        var orderId = UUID.randomUUID();
        when(placeCartOrderPort.placeOrder(any())).thenReturn(new PlacedOrder(orderId, BigDecimal.TEN, "MXN"));

        var repeatedSelection = java.util.stream.IntStream.range(0, 50)
                .mapToObj(i -> new SelectedItem(STORE_A, p1, null))
                .toList();
        var response = sut.checkout(new CheckoutCartCommand(BUYER_ID, repeatedSelection));

        var result = response.results().get(0);
        assertThat(result.succeeded()).isTrue();

        ArgumentCaptor<PlaceCartOrderCommand> captor = ArgumentCaptor.forClass(PlaceCartOrderCommand.class);
        verify(placeCartOrderPort, times(1)).placeOrder(captor.capture());
        assertThat(captor.getValue().lines()).hasSize(1);
        assertThat(captor.getValue().lines().get(0).quantity()).isEqualTo(5);
    }

    @Test
    void checkout_oneStoreThrowsDuringLoad_otherStoreStillSucceeds_bestEffortIsolation() {
        setUp();
        var pA = UUID.randomUUID();
        var pB = UUID.randomUUID();
        var cartA = Cart.of(BUYER_ID, STORE_A, List.of(CartItem.of(new CartLineKey(pA, null), 1)));
        when(cartStorePort.load(BUYER_ID, STORE_A)).thenReturn(cartA);
        when(cartStorePort.load(BUYER_ID, STORE_B)).thenThrow(new RuntimeException("redis unavailable"));
        when(loadCartProductInfoPort.loadForCart(any()))
                .thenReturn(Map.of(new CartLineRef(pA, null), info(pA, STORE_A, 10, false)));
        var orderId = UUID.randomUUID();
        when(placeCartOrderPort.placeOrder(any())).thenReturn(new PlacedOrder(orderId, BigDecimal.TEN, "MXN"));

        var response = sut.checkout(new CheckoutCartCommand(BUYER_ID, List.of(
                new SelectedItem(STORE_A, pA, null),
                new SelectedItem(STORE_B, pB, null))));

        assertThat(response.results()).hasSize(2);
        var resultA = response.results().stream().filter(r -> r.storeId().equals(STORE_A)).findFirst().orElseThrow();
        var resultB = response.results().stream().filter(r -> r.storeId().equals(STORE_B)).findFirst().orElseThrow();

        assertThat(resultA.succeeded()).isTrue();
        assertThat(resultA.orderId()).isEqualTo(orderId);

        assertThat(resultB.succeeded()).isFalse();
        assertThat(resultB.orderId()).isNull();
        assertThat(resultB.failureReason()).isNotNull();
    }

    @Test
    void checkout_onlyLineForStoreIsUnavailable_wholeStoreFails_noOrderCreated() {
        setUp();
        var p1 = UUID.randomUUID();
        var cart = Cart.of(BUYER_ID, STORE_A, List.of(CartItem.of(new CartLineKey(p1, null), 1)));
        when(cartStorePort.load(BUYER_ID, STORE_A)).thenReturn(cart);
        when(loadCartProductInfoPort.loadForCart(any())).thenReturn(Map.of());

        var response = sut.checkout(new CheckoutCartCommand(BUYER_ID, List.of(new SelectedItem(STORE_A, p1, null))));

        var result = response.results().get(0);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.failureReason()).isEqualTo("ALL_ITEMS_UNAVAILABLE");
        assertThat(result.orderId()).isNull();
        verify(placeCartOrderPort, never()).placeOrder(any());
    }

    @Test
    void checkout_storeSucceeds_publishesCartCheckoutCompletedEventWithSucceededTrue() {
        setUp();
        var p1 = UUID.randomUUID();
        var cart = Cart.of(BUYER_ID, STORE_A, List.of(CartItem.of(new CartLineKey(p1, null), 1)));
        when(cartStorePort.load(BUYER_ID, STORE_A)).thenReturn(cart);
        when(loadCartProductInfoPort.loadForCart(any()))
                .thenReturn(Map.of(new CartLineRef(p1, null), info(p1, STORE_A, 10, false)));
        var orderId = UUID.randomUUID();
        when(placeCartOrderPort.placeOrder(any())).thenReturn(new PlacedOrder(orderId, new BigDecimal("10.00"), "MXN"));

        sut.checkout(new CheckoutCartCommand(BUYER_ID, List.of(new SelectedItem(STORE_A, p1, null))));

        ArgumentCaptor<CartCheckoutCompletedEvent> captor = ArgumentCaptor.forClass(CartCheckoutCompletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        var event = captor.getValue();
        assertThat(event.buyerId()).isEqualTo(BUYER_ID);
        assertThat(event.storeId()).isEqualTo(STORE_A);
        assertThat(event.succeeded()).isTrue();
        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.failureReason()).isNull();
    }

    @Test
    void checkout_storeFails_publishesCartCheckoutCompletedEventWithSucceededFalseAndFailureReason() {
        setUp();
        var p1 = UUID.randomUUID();
        var cart = Cart.of(BUYER_ID, STORE_A, List.of(CartItem.of(new CartLineKey(p1, null), 1)));
        when(cartStorePort.load(BUYER_ID, STORE_A)).thenReturn(cart);
        when(loadCartProductInfoPort.loadForCart(any())).thenReturn(Map.of());

        sut.checkout(new CheckoutCartCommand(BUYER_ID, List.of(new SelectedItem(STORE_A, p1, null))));

        ArgumentCaptor<CartCheckoutCompletedEvent> captor = ArgumentCaptor.forClass(CartCheckoutCompletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        var event = captor.getValue();
        assertThat(event.buyerId()).isEqualTo(BUYER_ID);
        assertThat(event.storeId()).isEqualTo(STORE_A);
        assertThat(event.succeeded()).isFalse();
        assertThat(event.orderId()).isNull();
        assertThat(event.failureReason()).isEqualTo("ALL_ITEMS_UNAVAILABLE");
    }
}
