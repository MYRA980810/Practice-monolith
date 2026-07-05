package com.livecomerce.analytics.application;

import com.livecomerce.analytics.application.port.out.LoadLiveSummarySourcePort;
import com.livecomerce.analytics.application.port.out.LoadLiveSummarySourcePort.LiveSnapshot;
import com.livecomerce.analytics.application.port.out.LoadLiveSummarySourcePort.QualifyingOrder;
import com.livecomerce.analytics.application.port.out.SaveLiveSummaryPort;
import com.livecomerce.analytics.domain.LiveSummary;
import com.livecomerce.live.LiveEndedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveEndedSummaryListenerTest {

    @Mock SaveLiveSummaryPort saveLiveSummaryPort;
    @Mock LoadLiveSummarySourcePort loadLiveSummarySourcePort;

    @InjectMocks LiveEndedSummaryListener sut;

    private static final UUID LIVE_ID   = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID STORE_ID  = UUID.randomUUID();

    @Test
    void on_mixedPaidAndCancelledOrders_savesSnapshotWithAggregatedTotalsAndOrderDetails() {
        var startedAt = OffsetDateTime.parse("2026-07-01T10:00:00Z");
        var endedAt   = OffsetDateTime.parse("2026-07-01T11:00:00Z");
        when(saveLiveSummaryPort.existsByLiveId(LIVE_ID)).thenReturn(false);
        when(loadLiveSummarySourcePort.findLiveSnapshot(LIVE_ID))
                .thenReturn(Optional.of(new LiveSnapshot(STORE_ID, startedAt, endedAt, 42)));
        var order1Id = UUID.randomUUID();
        var buyer1Id = UUID.randomUUID();
        var order2Id = UUID.randomUUID();
        var buyer2Id = UUID.randomUUID();
        when(loadLiveSummarySourcePort.findQualifyingOrders(LIVE_ID)).thenReturn(List.of(
                new QualifyingOrder(order1Id, buyer1Id, "T-Shirt", new BigDecimal("100.00"), 5),
                new QualifyingOrder(order2Id, buyer2Id, "Mug, Cap", new BigDecimal("50.00"), 3)
        ));
        when(loadLiveSummarySourcePort.findTotalAllocated(LIVE_ID)).thenReturn(50L);
        when(saveLiveSummaryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.on(new LiveEndedEvent(LIVE_ID, SELLER_ID));

        var captor = ArgumentCaptor.forClass(LiveSummary.class);
        verify(saveLiveSummaryPort).save(captor.capture());
        var summary = captor.getValue();

        assertThat(summary.getLiveId()).isEqualTo(LIVE_ID);
        assertThat(summary.getSellerId()).isEqualTo(SELLER_ID);
        assertThat(summary.getStoreId()).isEqualTo(STORE_ID);
        assertThat(summary.getDurationSeconds()).isEqualTo(3600L);
        assertThat(summary.getPeakViewers()).isEqualTo(42);
        assertThat(summary.getTotalSales()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(summary.getOrderCount()).isEqualTo(2);
        assertThat(summary.getOrders()).hasSize(2);
        assertThat(summary.getOrders()).extracting("orderId").containsExactlyInAnyOrder(order1Id, order2Id);
        assertThat(summary.getOrders()).extracting("buyerId").containsExactlyInAnyOrder(buyer1Id, buyer2Id);
        assertThat(summary.getCurrency()).isEqualTo("MXN");
        assertThat(summary.getUnitsSold()).isEqualTo(8);
        assertThat(summary.getTotalAllocated()).isEqualTo(50);
    }

    @Test
    void on_zeroOrdersLive_stillSavesSummaryWithZeroTotals() {
        var startedAt = OffsetDateTime.parse("2026-07-01T10:00:00Z");
        var endedAt   = OffsetDateTime.parse("2026-07-01T10:30:00Z");
        when(saveLiveSummaryPort.existsByLiveId(LIVE_ID)).thenReturn(false);
        when(loadLiveSummarySourcePort.findLiveSnapshot(LIVE_ID))
                .thenReturn(Optional.of(new LiveSnapshot(STORE_ID, startedAt, endedAt, 5)));
        when(loadLiveSummarySourcePort.findQualifyingOrders(LIVE_ID)).thenReturn(List.of());
        when(loadLiveSummarySourcePort.findTotalAllocated(LIVE_ID)).thenReturn(0L);
        when(saveLiveSummaryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.on(new LiveEndedEvent(LIVE_ID, SELLER_ID));

        var captor = ArgumentCaptor.forClass(LiveSummary.class);
        verify(saveLiveSummaryPort).save(captor.capture());
        var summary = captor.getValue();

        assertThat(summary.getTotalSales()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getOrderCount()).isZero();
        assertThat(summary.getOrders()).isEmpty();
        assertThat(summary.getDurationSeconds()).isEqualTo(1800L);
        assertThat(summary.getCurrency()).isEqualTo("MXN");
        assertThat(summary.getUnitsSold()).isZero();
        assertThat(summary.getTotalAllocated()).isZero();
    }

    @Test
    void on_ordersWithNullCurrencySource_stillDefaultsToMxn() {
        var startedAt = OffsetDateTime.parse("2026-07-01T10:00:00Z");
        var endedAt   = OffsetDateTime.parse("2026-07-01T11:00:00Z");
        when(saveLiveSummaryPort.existsByLiveId(LIVE_ID)).thenReturn(false);
        when(loadLiveSummarySourcePort.findLiveSnapshot(LIVE_ID))
                .thenReturn(Optional.of(new LiveSnapshot(STORE_ID, startedAt, endedAt, 42)));
        when(loadLiveSummarySourcePort.findQualifyingOrders(LIVE_ID)).thenReturn(List.of(
                new QualifyingOrder(UUID.randomUUID(), UUID.randomUUID(), "T-Shirt", new BigDecimal("10.00"), 1)
        ));
        when(loadLiveSummarySourcePort.findTotalAllocated(LIVE_ID)).thenReturn(5L);
        when(saveLiveSummaryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sut.on(new LiveEndedEvent(LIVE_ID, SELLER_ID));

        var captor = ArgumentCaptor.forClass(LiveSummary.class);
        verify(saveLiveSummaryPort).save(captor.capture());
        assertThat(captor.getValue().getCurrency()).isEqualTo("MXN");
    }

    @Test
    void on_redeliveredEvent_skipsProcessingWithoutThrowingOrDuplicating() {
        when(saveLiveSummaryPort.existsByLiveId(LIVE_ID)).thenReturn(true);

        sut.on(new LiveEndedEvent(LIVE_ID, SELLER_ID));

        verify(saveLiveSummaryPort, times(1)).existsByLiveId(LIVE_ID);
        verify(loadLiveSummarySourcePort, never()).findLiveSnapshot(any());
        verify(loadLiveSummarySourcePort, never()).findQualifyingOrders(any());
        verify(saveLiveSummaryPort, never()).save(any());
    }
}
