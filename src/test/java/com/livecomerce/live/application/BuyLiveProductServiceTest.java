package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.BuyLiveProductUseCase.BuyLiveProductCommand;
import com.livecomerce.live.application.port.out.AtomicLiveProductStockPort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.LoadLiveProductPort;
import com.livecomerce.live.domain.*;
import com.livecomerce.order.LivePurchasePort;
import com.livecomerce.order.LivePurchasePort.LivePurchaseCommand;
import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderItemType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuyLiveProductServiceTest {

    @Mock LoadLiveProductPort        loadLiveProductPort;
    @Mock LoadLivePort               loadLivePort;
    @Mock LivePurchasePort           livePurchasePort;
    @Mock AtomicLiveProductStockPort atomicStockPort;
    @Mock LiveBroadcastService       broadcastService;
    @InjectMocks BuyLiveProductService sut;

    private static final UUID SELLER_ID  = UUID.randomUUID();
    private static final UUID STORE_ID   = UUID.randomUUID();
    private static final UUID BUYER_ID   = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID VARIANT_ID = UUID.randomUUID();

    private Live startedLive() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Live", null, null, 60);
        live.start();
        return live;
    }

    @Test
    void buyLiveProduct_catalogProduct_callsPlaceItemWithProductType() {
        var live = startedLive();
        var lp = LiveProduct.forCatalogProduct(live, PRODUCT_ID, VARIANT_ID, "Shirt", new BigDecimal("99"), "MXN", 10);
        var mockOrder = mock(Order.class);

        when(loadLiveProductPort.loadById(lp.getId())).thenReturn(Optional.of(lp));
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(atomicStockPort.atomicIncrementStockSold(lp.getId(), 2)).thenReturn(Optional.of(lp.getId()));
        when(livePurchasePort.placeItem(any())).thenReturn(mockOrder);

        var cmd = new BuyLiveProductCommand(live.getId(), lp.getId(), BUYER_ID, 2);
        var result = sut.buyLiveProduct(cmd);

        assertThat(result).isEqualTo(mockOrder);
        var captor = ArgumentCaptor.forClass(LivePurchaseCommand.class);
        verify(livePurchasePort).placeItem(captor.capture());
        assertThat(captor.getValue().itemType()).isEqualTo(OrderItemType.PRODUCT);
        assertThat(captor.getValue().quantity()).isEqualTo(2);
        assertThat(captor.getValue().buyerId()).isEqualTo(BUYER_ID);
    }

    @Test
    void buyLiveProduct_hotProduct_callsPlaceItemWithHotProductType() {
        var live = startedLive();
        var lp = LiveProduct.forHotProduct(live, "Mystery Box", new BigDecimal("50"), "MXN", 5, null);
        var mockOrder = mock(Order.class);

        when(loadLiveProductPort.loadById(lp.getId())).thenReturn(Optional.of(lp));
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(atomicStockPort.atomicIncrementStockSold(lp.getId(), 1)).thenReturn(Optional.of(lp.getId()));
        when(livePurchasePort.placeItem(any())).thenReturn(mockOrder);

        var cmd = new BuyLiveProductCommand(live.getId(), lp.getId(), BUYER_ID, 1);
        sut.buyLiveProduct(cmd);

        var captor = ArgumentCaptor.forClass(LivePurchaseCommand.class);
        verify(livePurchasePort).placeItem(captor.capture());
        assertThat(captor.getValue().itemType()).isEqualTo(OrderItemType.HOT_PRODUCT);
    }

    @Test
    void buyLiveProduct_liveNotActive_throws() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Live", null, null, 60); // SCHEDULED
        var lp = LiveProduct.forCatalogProduct(live, PRODUCT_ID, VARIANT_ID, "Shirt", new BigDecimal("99"), "MXN", 10);

        when(loadLiveProductPort.loadById(lp.getId())).thenReturn(Optional.of(lp));
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));

        var cmd = new BuyLiveProductCommand(live.getId(), lp.getId(), BUYER_ID, 1);

        assertThatThrownBy(() -> sut.buyLiveProduct(cmd))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void buyLiveProduct_atomicSqlReturnsEmpty_throwsOutOfStock() {
        var live = startedLive();
        var lp = LiveProduct.forCatalogProduct(live, PRODUCT_ID, VARIANT_ID, "Shirt", new BigDecimal("99"), "MXN", 1);

        when(loadLiveProductPort.loadById(lp.getId())).thenReturn(Optional.of(lp));
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(atomicStockPort.atomicIncrementStockSold(lp.getId(), 1)).thenReturn(Optional.empty());

        var cmd = new BuyLiveProductCommand(live.getId(), lp.getId(), BUYER_ID, 1);

        assertThatThrownBy(() -> sut.buyLiveProduct(cmd))
                .isInstanceOf(LiveProductOutOfStockException.class);
    }

    @Test
    void buyLiveProduct_placeItemFails_compensatesStock() {
        var live = startedLive();
        var lp = LiveProduct.forCatalogProduct(live, PRODUCT_ID, VARIANT_ID, "Shirt", new BigDecimal("99"), "MXN", 10);

        when(loadLiveProductPort.loadById(lp.getId())).thenReturn(Optional.of(lp));
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(atomicStockPort.atomicIncrementStockSold(lp.getId(), 1)).thenReturn(Optional.of(lp.getId()));
        when(livePurchasePort.placeItem(any())).thenThrow(new RuntimeException("payment error"));

        var cmd = new BuyLiveProductCommand(live.getId(), lp.getId(), BUYER_ID, 1);

        assertThatThrownBy(() -> sut.buyLiveProduct(cmd))
                .isInstanceOf(RuntimeException.class);
        verify(atomicStockPort).decrementStockSold(lp.getId(), 1);
    }
}
