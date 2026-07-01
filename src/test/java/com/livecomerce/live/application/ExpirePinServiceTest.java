package com.livecomerce.live.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.port.in.ExpirePinUseCase.ExpirePinCommand;
import com.livecomerce.live.application.port.out.AgoraRtmMessagePort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.LoadLiveProductPort;
import com.livecomerce.live.application.port.out.SaveLiveProductPort;
import com.livecomerce.live.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static com.livecomerce.live.domain.LiveProductStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpirePinServiceTest {

    @Mock LoadLivePort        loadLivePort;
    @Mock LoadLiveProductPort loadLiveProductPort;
    @Mock SaveLiveProductPort saveLiveProductPort;
    @Mock AgoraRtmMessagePort agoraRtmMessagePort;
    @Spy  ObjectMapper        objectMapper = new ObjectMapper();
    @InjectMocks ExpirePinService sut;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID STORE_ID  = UUID.randomUUID();

    private Live buildLive() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Test Live", null, null, 60);
        live.start();
        return live;
    }

    private LiveProduct pinnedWithStock(Live live, int allocated, int sold) {
        var lp = LiveProduct.forCatalogProduct(
                live, UUID.randomUUID(), UUID.randomUUID(), "Shirt", new BigDecimal("99"), "MXN", allocated);
        for (int i = 0; i < sold; i++) lp.incrementStockSold();
        lp.pin();
        return lp;
    }

    // ── 1. Stock exhausted → SOLD + RTM sent ─────────────────────────────────

    @Test
    void expirePin_whenStockExhausted_marksAsSold() throws Exception {
        var live = buildLive();
        var lp   = pinnedWithStock(live, 2, 2); // sold == allocated

        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(loadLiveProductPort.loadById(lp.getId())).thenReturn(Optional.of(lp));
        when(saveLiveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = sut.expirePin(new ExpirePinCommand(live.getId(), lp.getId(), SELLER_ID));

        assertThat(result.getStatus()).isEqualTo(SOLD);
        verify(agoraRtmMessagePort).sendChannelMessage(
                eq("live-chat:" + live.getId()),
                contains("\"status\":\"SOLD\""));
    }

    // ── 2. Stock remaining → AVAILABLE + RTM sent ────────────────────────────

    @Test
    void expirePin_whenStockRemaining_makesAvailable() throws Exception {
        var live = buildLive();
        var lp   = pinnedWithStock(live, 5, 2); // stock remaining

        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(loadLiveProductPort.loadById(lp.getId())).thenReturn(Optional.of(lp));
        when(saveLiveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = sut.expirePin(new ExpirePinCommand(live.getId(), lp.getId(), SELLER_ID));

        assertThat(result.getStatus()).isEqualTo(AVAILABLE);
        verify(agoraRtmMessagePort).sendChannelMessage(
                eq("live-chat:" + live.getId()),
                contains("\"status\":\"AVAILABLE\""));
    }

    // ── 3. Already AVAILABLE → idempotent, no RTM ────────────────────────────

    @Test
    void expirePin_whenAlreadyAvailable_isIdempotent() {
        var live = buildLive();
        var lp   = LiveProduct.forCatalogProduct(
                live, UUID.randomUUID(), UUID.randomUUID(), "Shirt", new BigDecimal("99"), "MXN", 5);
        // status = AVAILABLE (not pinned)

        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(loadLiveProductPort.loadById(lp.getId())).thenReturn(Optional.of(lp));

        var result = sut.expirePin(new ExpirePinCommand(live.getId(), lp.getId(), SELLER_ID));

        assertThat(result.getStatus()).isEqualTo(AVAILABLE);
        verifyNoInteractions(agoraRtmMessagePort);
        verify(saveLiveProductPort, never()).save(any());
    }

    // ── 4. Already SOLD → idempotent, no RTM ────────────────────────────────

    @Test
    void expirePin_whenAlreadySold_isIdempotent() {
        var live = buildLive();
        var lp   = LiveProduct.forCatalogProduct(
                live, UUID.randomUUID(), UUID.randomUUID(), "Shirt", new BigDecimal("99"), "MXN", 1);
        lp.pin();
        lp.markAsSold();

        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(loadLiveProductPort.loadById(lp.getId())).thenReturn(Optional.of(lp));

        var result = sut.expirePin(new ExpirePinCommand(live.getId(), lp.getId(), SELLER_ID));

        assertThat(result.getStatus()).isEqualTo(SOLD);
        verifyNoInteractions(agoraRtmMessagePort);
        verify(saveLiveProductPort, never()).save(any());
    }
}
