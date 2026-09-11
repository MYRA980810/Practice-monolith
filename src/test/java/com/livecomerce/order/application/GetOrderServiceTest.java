package com.livecomerce.order.application;

import com.livecomerce.order.application.port.out.LoadLiveProductImagePort;
import com.livecomerce.order.application.port.out.LoadOrderPort;
import com.livecomerce.order.application.port.out.LoadStoreIdPort;
import com.livecomerce.order.domain.Order;
import com.livecomerce.order.domain.OrderItemType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOrderServiceTest {

    @Mock LoadOrderPort loadOrderPort;
    @Mock LoadLiveProductImagePort loadLiveProductImagePort;
    @Mock LoadStoreIdPort loadStoreIdPort;

    @InjectMocks GetOrderService service;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID LIVE_ID  = UUID.randomUUID();

    private Order buildOrderWithItem(UUID productId) {
        var order = Order.open(BUYER_ID, STORE_ID, LIVE_ID, "MXN");
        order.addItem(productId, UUID.randomUUID(), "Producto", new BigDecimal("100"), "MXN", 1, 10, OrderItemType.PRODUCT);
        return order;
    }

    @Test
    void getById_asBuyer_returnsOrder() {
        var order = buildOrderWithItem(UUID.randomUUID());
        when(loadOrderPort.loadById(order.getId())).thenReturn(Optional.of(order));

        var result = service.getById(order.getId(), BUYER_ID);

        assertThat(result).isEqualTo(order);
    }

    @Test
    void getById_asOwningSeller_returnsOrder() {
        var order = buildOrderWithItem(UUID.randomUUID());
        when(loadOrderPort.loadById(order.getId())).thenReturn(Optional.of(order));
        when(loadStoreIdPort.findStoreIdByUserId(SELLER_ID)).thenReturn(Optional.of(STORE_ID));

        var result = service.getById(order.getId(), SELLER_ID);

        assertThat(result).isEqualTo(order);
    }

    @Test
    void getById_asUnrelatedUser_throwsAccessDenied() {
        var order = buildOrderWithItem(UUID.randomUUID());
        var strangerId = UUID.randomUUID();
        when(loadOrderPort.loadById(order.getId())).thenReturn(Optional.of(order));
        when(loadStoreIdPort.findStoreIdByUserId(strangerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(order.getId(), strangerId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getById_asSellerOfDifferentStore_throwsAccessDenied() {
        var order = buildOrderWithItem(UUID.randomUUID());
        var otherSellerId = UUID.randomUUID();
        when(loadOrderPort.loadById(order.getId())).thenReturn(Optional.of(order));
        when(loadStoreIdPort.findStoreIdByUserId(otherSellerId)).thenReturn(Optional.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.getById(order.getId(), otherSellerId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getReadyToShipByLive_returnsOrdersWithBatchedImageLookup() {
        var productId = UUID.randomUUID();
        var order = buildOrderWithItem(productId);
        when(loadStoreIdPort.findStoreIdByUserId(SELLER_ID)).thenReturn(Optional.of(STORE_ID));
        when(loadOrderPort.loadReadyToShipByLive(STORE_ID, LIVE_ID)).thenReturn(List.of(order));
        when(loadLiveProductImagePort.findImageUrls(LIVE_ID, Set.of(productId)))
                .thenReturn(Map.of(productId, "https://cdn.test/img.jpg"));

        var result = service.getReadyToShipByLive(SELLER_ID, LIVE_ID);

        assertThat(result.orders()).containsExactly(order);
        assertThat(result.imageUrlByProductId()).containsEntry(productId, "https://cdn.test/img.jpg");
        verify(loadLiveProductImagePort).findImageUrls(LIVE_ID, Set.of(productId));
    }

    @Test
    void getReadyToShipByLive_noOrders_skipsImageLookup() {
        when(loadStoreIdPort.findStoreIdByUserId(SELLER_ID)).thenReturn(Optional.of(STORE_ID));
        when(loadOrderPort.loadReadyToShipByLive(STORE_ID, LIVE_ID)).thenReturn(List.of());

        var result = service.getReadyToShipByLive(SELLER_ID, LIVE_ID);

        assertThat(result.orders()).isEmpty();
        assertThat(result.imageUrlByProductId()).isEmpty();
        verify(loadLiveProductImagePort, never()).findImageUrls(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getReadyToShipByLive_multipleOrdersSameProduct_singleBatchedImageCall() {
        var productId = UUID.randomUUID();
        var order1 = buildOrderWithItem(productId);
        var order2 = buildOrderWithItem(productId);
        when(loadStoreIdPort.findStoreIdByUserId(SELLER_ID)).thenReturn(Optional.of(STORE_ID));
        when(loadOrderPort.loadReadyToShipByLive(STORE_ID, LIVE_ID)).thenReturn(List.of(order1, order2));
        when(loadLiveProductImagePort.findImageUrls(LIVE_ID, Set.of(productId)))
                .thenReturn(Map.of(productId, "https://cdn.test/img.jpg"));

        service.getReadyToShipByLive(SELLER_ID, LIVE_ID);

        verify(loadLiveProductImagePort, org.mockito.Mockito.times(1)).findImageUrls(LIVE_ID, Set.of(productId));
    }

    @Test
    void getReadyToShipByLive_sellerWithoutStore_throwsStoreNotFound() {
        when(loadStoreIdPort.findStoreIdByUserId(SELLER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReadyToShipByLive(SELLER_ID, LIVE_ID))
                .isInstanceOf(StoreNotFoundException.class);
        verify(loadOrderPort, never()).loadReadyToShipByLive(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
