package com.livecomerce.live.application;

import com.livecomerce.live.BuyerDefaultAddressPort;
import com.livecomerce.live.SellerShippingAddressPort;
import com.livecomerce.live.application.port.in.ExitLiveUseCase.ExitLiveCommand;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.order.LiveOrderFinalizePort;
import com.livecomerce.order.LiveOrderFinalizePort.FinalizeCommand;
import com.livecomerce.order.domain.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExitLiveServiceTest {

    @Mock LiveOrderFinalizePort liveOrderFinalizePort;
    @Mock LoadLivePort loadLivePort;
    @Mock BuyerDefaultAddressPort buyerDefaultAddressPort;
    @Mock SellerShippingAddressPort sellerShippingAddressPort;

    @InjectMocks ExitLiveService sut;

    private static final UUID LIVE_ID   = UUID.randomUUID();
    private static final UUID BUYER_ID  = UUID.randomUUID();
    private static final UUID STORE_ID  = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();

    private Live buildLive() {
        return Live.create(SELLER_ID, STORE_ID, com.livecomerce.live.domain.LiveContext.STORE, "Test", null, null, 60);
    }

    @Test
    void exitLive_noActiveOrder_returnsResponseWithNullOrder() {
        when(liveOrderFinalizePort.loadActiveOrderForBuyerAndLive(BUYER_ID, LIVE_ID))
                .thenReturn(Optional.empty());

        var cmd    = new ExitLiveCommand(LIVE_ID, BUYER_ID, null, null);
        var result = sut.exitLive(cmd);

        assertThat(result.order()).isNull();
        verify(liveOrderFinalizePort, never()).finalize(any());
    }

    @Test
    @SuppressWarnings("null")
    void exitLive_withActiveOrder_andBothAddresses_returnsFullResponse() {
        var mockOrder      = mock(Order.class);
        var finalizedOrder = mock(Order.class);
        var live           = buildLive();
        var buyerAddr      = new BuyerDefaultAddressPort.ShippingAddress(
                "Av. Corrientes", "1234", null, "San Nicolás", "CABA", "CABA", "C1043", "AR");
        var sellerAddr     = new SellerShippingAddressPort.ShippingAddress(
                "Calle Falsa", "123", null, "Col", "CDMX", "Ciudad", "01000", "MX");

        when(mockOrder.getId()).thenReturn(UUID.randomUUID());
        when(liveOrderFinalizePort.loadActiveOrderForBuyerAndLive(BUYER_ID, LIVE_ID))
                .thenReturn(Optional.of(mockOrder));
        when(liveOrderFinalizePort.finalize(any())).thenReturn(finalizedOrder);
        when(loadLivePort.loadById(LIVE_ID)).thenReturn(Optional.of(live));
        when(buyerDefaultAddressPort.loadDefaultAddress(BUYER_ID)).thenReturn(Optional.of(buyerAddr));
        when(sellerShippingAddressPort.loadByStoreId(STORE_ID)).thenReturn(Optional.of(sellerAddr));

        var cmd    = new ExitLiveCommand(LIVE_ID, BUYER_ID, null, null);
        var result = sut.exitLive(cmd);

        assertThat(result.order()).isEqualTo(finalizedOrder);
        assertThat(result.buyerAddress()).isEqualTo(buyerAddr);
        assertThat(result.sellerDispatchAddress()).isEqualTo(sellerAddr);

        var captor = ArgumentCaptor.forClass(FinalizeCommand.class);
        verify(liveOrderFinalizePort).finalize(captor.capture());
        assertThat(captor.getValue().buyerId()).isEqualTo(BUYER_ID);
    }

    @Test
    @SuppressWarnings("null")
    void exitLive_withShippingAddressInCommand_usesCommandAddress() {
        var mockOrder      = mock(Order.class);
        var finalizedOrder = mock(Order.class);
        var live           = buildLive();

        when(mockOrder.getId()).thenReturn(UUID.randomUUID());
        when(liveOrderFinalizePort.loadActiveOrderForBuyerAndLive(BUYER_ID, LIVE_ID))
                .thenReturn(Optional.of(mockOrder));
        when(liveOrderFinalizePort.finalize(any())).thenReturn(finalizedOrder);
        when(loadLivePort.loadById(LIVE_ID)).thenReturn(Optional.of(live));
        when(sellerShippingAddressPort.loadByStoreId(STORE_ID)).thenReturn(Optional.empty());

        var cmd    = new ExitLiveCommand(LIVE_ID, BUYER_ID, "Av. Corrientes 1234", null);
        var result = sut.exitLive(cmd);

        assertThat(result.order()).isEqualTo(finalizedOrder);

        var captor = ArgumentCaptor.forClass(FinalizeCommand.class);
        verify(liveOrderFinalizePort).finalize(captor.capture());
        assertThat(captor.getValue().shippingAddress()).isEqualTo("Av. Corrientes 1234");
        verify(buyerDefaultAddressPort, never()).loadDefaultAddress(any());
    }
}
