package com.livecomerce.cart.application;

import com.livecomerce.cart.CartItemDepletedEvent;
import com.livecomerce.cart.application.port.out.CartStorePort;
import com.livecomerce.cart.domain.Cart;
import com.livecomerce.cart.domain.CartItem;
import com.livecomerce.cart.domain.CartLineKey;
import com.livecomerce.catalog.LoadCartProductInfoPort;
import com.livecomerce.catalog.LoadCartProductInfoPort.CartLineRef;
import com.livecomerce.catalog.LoadCartProductInfoPort.CartProductInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartStockDepletionServiceTest {

    @Mock CartStorePort cartStorePort;
    @Mock LoadCartProductInfoPort loadCartProductInfoPort;
    @Mock ApplicationEventPublisher eventPublisher;

    CartStockDepletionService sut;

    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    private void setUp() {
        sut = new CartStockDepletionService(cartStorePort, loadCartProductInfoPort, eventPublisher);
    }

    private void givenCartWithStock(int availableStock) {
        var cart = Cart.of(BUYER_ID, STORE_ID, List.of(CartItem.of(new CartLineKey(PRODUCT_ID, null), 1)));
        when(cartStorePort.loadStoreIds(BUYER_ID)).thenReturn(Set.of(STORE_ID));
        when(cartStorePort.load(BUYER_ID, STORE_ID)).thenReturn(cart);
        var ref = new CartLineRef(PRODUCT_ID, null);
        var info = new CartProductInfo(PRODUCT_ID, null, STORE_ID, "Playera", null,
                BigDecimal.TEN, "MXN", availableStock, true, false, false);
        when(loadCartProductInfoPort.loadForCart(any())).thenReturn(Map.of(ref, info));
    }

    @Test
    void firstCrossing_fires_andRecordsThreshold() {
        setUp();
        givenCartWithStock(4); // crosses "low stock" (<=5)
        when(cartStorePort.loadNotifiedThreshold(BUYER_ID, STORE_ID, PRODUCT_ID, null)).thenReturn(null);

        sut.checkBuyerCarts(BUYER_ID);

        ArgumentCaptor<CartItemDepletedEvent> captor = ArgumentCaptor.forClass(CartItemDepletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().threshold()).isEqualTo(5);
        assertThat(captor.getValue().availableStock()).isEqualTo(4);
        verify(cartStorePort).recordNotifiedThreshold(BUYER_ID, STORE_ID, PRODUCT_ID, null, 5);
    }

    @Test
    void stillBelowSameThreshold_onSubsequentTick_doesNotRefire() {
        setUp();
        givenCartWithStock(3); // still <=5, not <=0
        when(cartStorePort.loadNotifiedThreshold(BUYER_ID, STORE_ID, PRODUCT_ID, null)).thenReturn(5);

        sut.checkBuyerCarts(BUYER_ID);

        verify(eventPublisher, never()).publishEvent(any());
        verify(cartStorePort, never()).recordNotifiedThreshold(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void newLowerThreshold_firesAgain() {
        setUp();
        givenCartWithStock(0); // crosses "out of stock"
        when(cartStorePort.loadNotifiedThreshold(BUYER_ID, STORE_ID, PRODUCT_ID, null)).thenReturn(5);

        sut.checkBuyerCarts(BUYER_ID);

        ArgumentCaptor<CartItemDepletedEvent> captor = ArgumentCaptor.forClass(CartItemDepletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().threshold()).isEqualTo(0);
        verify(cartStorePort).recordNotifiedThreshold(BUYER_ID, STORE_ID, PRODUCT_ID, null, 0);
    }

    @Test
    void stockAboveAllThresholds_noCrossing_doesNotFire() {
        setUp();
        givenCartWithStock(50);

        sut.checkBuyerCarts(BUYER_ID);

        verify(eventPublisher, never()).publishEvent(any());
        verify(cartStorePort, never()).loadNotifiedThreshold(any(), any(), any(), any());
    }

    @Test
    void catalogLookupFailedForLine_skipsSafely_doesNotThrowOrFire() {
        setUp();
        var cart = Cart.of(BUYER_ID, STORE_ID, List.of(CartItem.of(new CartLineKey(PRODUCT_ID, null), 1)));
        when(cartStorePort.loadStoreIds(BUYER_ID)).thenReturn(Set.of(STORE_ID));
        when(cartStorePort.load(BUYER_ID, STORE_ID)).thenReturn(cart);
        when(loadCartProductInfoPort.loadForCart(any())).thenReturn(Map.of());

        sut.checkBuyerCarts(BUYER_ID);

        verify(eventPublisher, never()).publishEvent(any());
    }
}
