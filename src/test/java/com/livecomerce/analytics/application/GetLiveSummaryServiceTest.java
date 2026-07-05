package com.livecomerce.analytics.application;

import com.livecomerce.analytics.application.port.out.LoadLiveSummaryPort;
import com.livecomerce.analytics.domain.LiveSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetLiveSummaryServiceTest {

    @Mock
    LoadLiveSummaryPort loadLiveSummaryPort;

    GetLiveSummaryService service;

    private static final UUID LIVE_ID = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();

    private static LiveSummary buildSummary(UUID storeId) {
        return LiveSummary.create(
                LIVE_ID, SELLER_ID, storeId,
                OffsetDateTime.now().minusHours(1), OffsetDateTime.now(),
                3600, 42);
    }

    void setUp() {
        service = new GetLiveSummaryService(loadLiveSummaryPort);
    }

    @Test
    void getSummary_ownerStore_returnsSummary() {
        setUp();
        var summary = buildSummary(STORE_ID);
        when(loadLiveSummaryPort.findByLiveId(LIVE_ID)).thenReturn(Optional.of(summary));

        var result = service.getSummary(LIVE_ID, STORE_ID);

        assertThat(result).isSameAs(summary);
    }

    @Test
    void getSummary_absent_throwsNotFound() {
        setUp();
        when(loadLiveSummaryPort.findByLiveId(LIVE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSummary(LIVE_ID, STORE_ID))
                .isInstanceOf(LiveSummaryNotFoundException.class);
    }

    @Test
    void getSummary_differentStore_throwsNotOwned() {
        setUp();
        var summary = buildSummary(STORE_ID);
        when(loadLiveSummaryPort.findByLiveId(LIVE_ID)).thenReturn(Optional.of(summary));

        assertThatThrownBy(() -> service.getSummary(LIVE_ID, UUID.randomUUID()))
                .isInstanceOf(LiveSummaryNotOwnedException.class);
    }
}
