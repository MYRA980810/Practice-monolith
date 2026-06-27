package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.PinProductUseCase.PinProductCommand;
import com.livecomerce.live.application.port.out.*;
import com.livecomerce.live.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PinProductServiceTest {

    @Mock LoadLivePort              loadLivePort;
    @Mock LoadLiveProductPort       loadLiveProductPort;
    @Mock SaveLiveProductPort       saveLiveProductPort;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock LiveBroadcastService      broadcastService;
    @InjectMocks PinProductService sut;

    private static final UUID SELLER_ID  = UUID.randomUUID();
    private static final UUID STORE_ID   = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID VARIANT_ID = UUID.randomUUID();

    private Live liveLive() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Live", null, null, 60);
        live.start();
        return live;
    }

    @Test
    @SuppressWarnings("null") // Mockito capture() is @Nullable; publishEvent takes @NonNull — false positive
    void pinProduct_noPreviousPinned_pinsAndPublishesEvent() {
        var live  = liveLive();
        var lp = LiveProduct.forCatalogProduct(live, PRODUCT_ID, VARIANT_ID, "Shirt", new BigDecimal("99"), "MXN", 10);

        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(loadLiveProductPort.loadById(lp.getId())).thenReturn(Optional.of(lp));
        when(loadLiveProductPort.loadPinnedByLiveId(live.getId())).thenReturn(Optional.empty());
        when(saveLiveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = sut.pinProduct(new PinProductCommand(live.getId(), lp.getId(), SELLER_ID));

        assertThat(result.isPinned()).isTrue();
        verify(eventPublisher).publishEvent(any(com.livecomerce.live.LiveProductPinnedEvent.class));
    }

    @Test
    void pinProduct_withPreviousPinned_unpinsPreviousAndPinsNew() {
        var live     = liveLive();
        var oldPinned = LiveProduct.forCatalogProduct(live, UUID.randomUUID(), UUID.randomUUID(), "Old", new BigDecimal("50"), "MXN", 5);
        var newPinned = LiveProduct.forCatalogProduct(live, PRODUCT_ID, VARIANT_ID, "New", new BigDecimal("99"), "MXN", 10);
        oldPinned.pin();

        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(loadLiveProductPort.loadById(newPinned.getId())).thenReturn(Optional.of(newPinned));
        when(loadLiveProductPort.loadPinnedByLiveId(live.getId())).thenReturn(Optional.of(oldPinned));
        when(saveLiveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.pinProduct(new PinProductCommand(live.getId(), newPinned.getId(), SELLER_ID));

        assertThat(oldPinned.isPinned()).isFalse();
        assertThat(newPinned.isPinned()).isTrue();
        verify(saveLiveProductPort, times(2)).save(any());
    }

    @Test
    void pinProduct_wrongSeller_throwsLiveNotOwned() {
        var live = liveLive();
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));

        assertThatThrownBy(() -> sut.pinProduct(
                new PinProductCommand(live.getId(), UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(LiveNotOwnedBySellerException.class);
    }
}
