package com.livecomerce.cart.infrastructure.scheduler;

import com.livecomerce.cart.application.CartStockDepletionService;
import com.livecomerce.cart.application.port.out.CartStorePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartStockDepletionJobTest {

    @Mock CartStorePort cartStorePort;
    @Mock CartStockDepletionService cartStockDepletionService;

    CartStockDepletionJob job;

    @Test
    void checkDepletion_sweepsEveryBuyerReturnedByScan() {
        job = new CartStockDepletionJob(cartStorePort, cartStockDepletionService);
        var buyer1 = UUID.randomUUID();
        var buyer2 = UUID.randomUUID();
        when(cartStorePort.scanAllBuyerIds()).thenReturn(Set.of(buyer1, buyer2));

        job.checkDepletion();

        verify(cartStockDepletionService).checkBuyerCarts(buyer1);
        verify(cartStockDepletionService).checkBuyerCarts(buyer2);
    }

    @Test
    void checkDepletion_oneBuyerThrows_othersStillProcessed() {
        job = new CartStockDepletionJob(cartStorePort, cartStockDepletionService);
        var buyer1 = UUID.randomUUID();
        var buyer2 = UUID.randomUUID();
        when(cartStorePort.scanAllBuyerIds()).thenReturn(Set.of(buyer1, buyer2));
        doThrow(new RuntimeException("boom")).when(cartStockDepletionService).checkBuyerCarts(buyer1);

        job.checkDepletion();

        verify(cartStockDepletionService).checkBuyerCarts(buyer1);
        verify(cartStockDepletionService).checkBuyerCarts(buyer2);
    }

    @Test
    void checkDepletion_noBuyers_doesNothing() {
        job = new CartStockDepletionJob(cartStorePort, cartStockDepletionService);
        when(cartStorePort.scanAllBuyerIds()).thenReturn(Set.of());

        job.checkDepletion();

        verify(cartStockDepletionService, times(0)).checkBuyerCarts(any());
    }
}
