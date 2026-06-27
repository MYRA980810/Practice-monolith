package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.AddCatalogProductUseCase.AddCatalogProductCommand;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveLiveProductPort;
import com.livecomerce.live.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class AddCatalogProductToLiveServiceTest {

    @Mock LoadLivePort        loadLivePort;
    @Mock SaveLiveProductPort saveLiveProductPort;
    @InjectMocks AddCatalogProductToLiveService sut;

    private static final UUID SELLER_ID  = UUID.randomUUID();
    private static final UUID STORE_ID   = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID VARIANT_ID = UUID.randomUUID();

    @Test
    void addCatalogProduct_toScheduledLive_succeeds() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Live", null, null, 60);
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(saveLiveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new AddCatalogProductCommand(
                live.getId(), SELLER_ID, PRODUCT_ID, VARIANT_ID,
                "T-Shirt", new BigDecimal("199.00"), "MXN", 50);
        var result = sut.addCatalogProduct(cmd);

        assertThat(result.isHot()).isFalse();
        assertThat(result.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(result.getProductNameSnapshot()).isEqualTo("T-Shirt");
        assertThat(result.getStockAllocated()).isEqualTo(50);
    }

    @Test
    void addCatalogProduct_toLiveLive_succeeds() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Live", null, null, 60);
        live.start();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(saveLiveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new AddCatalogProductCommand(
                live.getId(), SELLER_ID, PRODUCT_ID, VARIANT_ID,
                "Jeans", new BigDecimal("499.00"), "MXN", 20);
        var result = sut.addCatalogProduct(cmd);

        assertThat(result.getProductNameSnapshot()).isEqualTo("Jeans");
    }

    @Test
    void addCatalogProduct_wrongSeller_throwsLiveNotOwned() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Live", null, null, 60);
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));

        var cmd = new AddCatalogProductCommand(
                live.getId(), UUID.randomUUID(), PRODUCT_ID, VARIANT_ID,
                "T-Shirt", new BigDecimal("199.00"), "MXN", 50);

        assertThatThrownBy(() -> sut.addCatalogProduct(cmd))
                .isInstanceOf(LiveNotOwnedBySellerException.class);
    }

    @Test
    void addCatalogProduct_toEndedLive_throwsIllegalState() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Live", null, null, 60);
        live.start();
        live.end();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));

        var cmd = new AddCatalogProductCommand(
                live.getId(), SELLER_ID, PRODUCT_ID, VARIANT_ID,
                "T-Shirt", new BigDecimal("199.00"), "MXN", 50);

        assertThatThrownBy(() -> sut.addCatalogProduct(cmd))
                .isInstanceOf(IllegalStateException.class);
    }
}
