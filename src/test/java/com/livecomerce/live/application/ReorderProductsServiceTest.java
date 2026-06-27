package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.ReorderProductsUseCase.ReorderProductsCommand;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.LoadLiveProductPort;
import com.livecomerce.live.application.port.out.SaveLiveProductPort;
import com.livecomerce.live.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReorderProductsServiceTest {

    @Mock LoadLivePort        loadLivePort;
    @Mock LoadLiveProductPort loadLiveProductPort;
    @Mock SaveLiveProductPort saveLiveProductPort;
    @InjectMocks ReorderProductsService sut;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID STORE_ID  = UUID.randomUUID();

    @Test
    void reorderProducts_updatesPositionsByIndex() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Live", null, null, 60);
        var p1 = LiveProduct.forCatalogProduct(live, UUID.randomUUID(), UUID.randomUUID(), "P1", new BigDecimal("10"), "MXN", 5);
        var p2 = LiveProduct.forCatalogProduct(live, UUID.randomUUID(), UUID.randomUUID(), "P2", new BigDecimal("20"), "MXN", 5);
        var p3 = LiveProduct.forCatalogProduct(live, UUID.randomUUID(), UUID.randomUUID(), "P3", new BigDecimal("30"), "MXN", 5);

        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(loadLiveProductPort.loadByLiveId(live.getId())).thenReturn(List.of(p1, p2, p3));
        when(saveLiveProductPort.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // Reorder: p3 first, p1 second, p2 third
        var cmd = new ReorderProductsCommand(live.getId(), SELLER_ID, List.of(p3.getId(), p1.getId(), p2.getId()));
        var result = sut.reorderProducts(cmd);

        assertThat(p3.getPosition()).isZero();
        assertThat(p1.getPosition()).isEqualTo(1);
        assertThat(p2.getPosition()).isEqualTo(2);
        assertThat(result).hasSize(3);
    }

    @Test
    void reorderProducts_unknownId_throwsIllegalArgument() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Live", null, null, 60);
        var p1 = LiveProduct.forCatalogProduct(live, UUID.randomUUID(), UUID.randomUUID(), "P1", new BigDecimal("10"), "MXN", 5);

        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(loadLiveProductPort.loadByLiveId(live.getId())).thenReturn(List.of(p1));

        var unknownId = UUID.randomUUID();
        var cmd = new ReorderProductsCommand(live.getId(), SELLER_ID, List.of(p1.getId(), unknownId));

        assertThatThrownBy(() -> sut.reorderProducts(cmd))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reorderProducts_wrongSeller_throwsLiveNotOwned() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Live", null, null, 60);
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));

        assertThatThrownBy(() -> sut.reorderProducts(
                new ReorderProductsCommand(live.getId(), UUID.randomUUID(), List.of())))
                .isInstanceOf(LiveNotOwnedBySellerException.class);
    }
}
