package com.livecomerce.analytics.application;

import com.livecomerce.analytics.application.port.out.LoadLiveSummaryPort;
import com.livecomerce.analytics.application.port.out.LoadLiveSummarySourcePort;
import com.livecomerce.analytics.domain.LiveSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class GetLiveSummaryServiceTest {

    @Mock
    LoadLiveSummaryPort loadLiveSummaryPort;

    @Mock
    LoadLiveSummarySourcePort loadLiveSummarySourcePort;

    GetLiveSummaryService service;

    private static final UUID LIVE_ID = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID BUYER_ID = UUID.randomUUID();

    private static LiveSummary buildSummary(UUID storeId) {
        var summary = LiveSummary.create(
                LIVE_ID, SELLER_ID, storeId,
                OffsetDateTime.now().minusHours(1), OffsetDateTime.now(),
                3600, 42);
        summary.addOrder(UUID.randomUUID(), BUYER_ID, "T-Shirt", new BigDecimal("100.00"));
        return summary;
    }

    void setUp() {
        service = new GetLiveSummaryService(loadLiveSummaryPort, loadLiveSummarySourcePort);
    }

    @Test
    void getSummary_ownerStore_returnsSummaryWithBatchedBuyerNames() {
        setUp();
        var summary = buildSummary(STORE_ID);
        when(loadLiveSummaryPort.findByLiveId(LIVE_ID)).thenReturn(Optional.of(summary));
        when(loadLiveSummarySourcePort.findBuyerNames(Set.of(BUYER_ID))).thenReturn(Map.of(BUYER_ID, "Jane Doe"));

        var result = service.getSummary(LIVE_ID, STORE_ID);

        assertThat(result.summary()).isSameAs(summary);
        assertThat(result.buyerNames()).containsEntry(BUYER_ID, "Jane Doe");
        verify(loadLiveSummarySourcePort).findBuyerNames(Set.of(BUYER_ID));
    }

    @Test
    void getSummary_absent_throwsNotFound() {
        setUp();
        when(loadLiveSummaryPort.findByLiveId(LIVE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSummary(LIVE_ID, STORE_ID))
                .isInstanceOf(LiveSummaryNotFoundException.class);
        verify(loadLiveSummarySourcePort, never()).findBuyerNames(any());
    }

    @Test
    void getSummary_differentStore_throwsNotOwned() {
        setUp();
        var summary = buildSummary(STORE_ID);
        when(loadLiveSummaryPort.findByLiveId(LIVE_ID)).thenReturn(Optional.of(summary));

        assertThatThrownBy(() -> service.getSummary(LIVE_ID, UUID.randomUUID()))
                .isInstanceOf(LiveSummaryNotOwnedException.class);
        verify(loadLiveSummarySourcePort, never()).findBuyerNames(any());
    }
}
