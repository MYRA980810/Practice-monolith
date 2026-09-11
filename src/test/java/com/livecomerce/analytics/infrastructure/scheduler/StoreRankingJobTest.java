package com.livecomerce.analytics.infrastructure.scheduler;

import com.livecomerce.analytics.LoadStoreRatingPort;
import com.livecomerce.analytics.LoadStoreRatingPort.StoreRatingSummary;
import com.livecomerce.analytics.application.port.out.LoadRecentSalesVolumePort;
import com.livecomerce.analytics.application.port.out.SaveStoreRankingPort;
import com.livecomerce.analytics.domain.StoreRankingSnapshot;
import com.livecomerce.store.application.port.in.GetStoreFollowersUseCase;
import com.livecomerce.store.application.port.in.ListStoresUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreRankingJobTest {

    @Mock ListStoresUseCase listStoresUseCase;
    @Mock LoadStoreRatingPort loadStoreRatingPort;
    @Mock GetStoreFollowersUseCase getStoreFollowersUseCase;
    @Mock LoadRecentSalesVolumePort loadRecentSalesVolumePort;
    @Mock SaveStoreRankingPort saveStoreRankingPort;

    @InjectMocks StoreRankingJob job;

    @BeforeEach
    void setUpConfig() {
        ReflectionTestUtils.setField(job, "minReviews", 3);
        ReflectionTestUtils.setField(job, "recentSalesWindowDays", 30);
        ReflectionTestUtils.setField(job, "weightRating", 0.5);
        ReflectionTestUtils.setField(job, "weightFollowers", 0.25);
        ReflectionTestUtils.setField(job, "weightSales", 0.25);
    }

    @Test
    void computeRanking_whenNoActiveStores_doesNothing() {
        when(listStoresUseCase.listActiveIds()).thenReturn(List.of());

        job.computeRanking();

        verify(saveStoreRankingPort, never()).replaceAll(any());
    }

    @Test
    void computeRanking_excludesStoreBelowMinReviews() {
        var eligible = UUID.randomUUID();
        var tooFewReviews = UUID.randomUUID();
        when(listStoresUseCase.listActiveIds()).thenReturn(List.of(eligible, tooFewReviews));
        when(loadStoreRatingPort.loadSummaries(any())).thenReturn(Map.of(
                eligible, new StoreRatingSummary(4.0, 5L),
                tooFewReviews, new StoreRatingSummary(5.0, 1L) // below minReviews=3
        ));
        when(getStoreFollowersUseCase.getFollowerCounts(any())).thenReturn(Map.of());
        when(loadRecentSalesVolumePort.loadRecentSalesVolume(any(), any(), any())).thenReturn(Map.of());

        job.computeRanking();

        var captor = ArgumentCaptor.forClass(List.class);
        verify(saveStoreRankingPort).replaceAll(captor.capture());
        @SuppressWarnings("unchecked")
        List<StoreRankingSnapshot> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getStoreId()).isEqualTo(eligible);
    }

    @Test
    void computeRanking_ordersByCompositeScoreDescending() {
        var storeA = UUID.randomUUID(); // higher rating, followers, sales -> should rank 1
        var storeB = UUID.randomUUID();
        when(listStoresUseCase.listActiveIds()).thenReturn(List.of(storeA, storeB));
        when(loadStoreRatingPort.loadSummaries(any())).thenReturn(Map.of(
                storeA, new StoreRatingSummary(5.0, 5L),
                storeB, new StoreRatingSummary(3.0, 5L)
        ));
        when(getStoreFollowersUseCase.getFollowerCounts(any())).thenReturn(Map.of(
                storeA, 100L,
                storeB, 10L
        ));
        when(loadRecentSalesVolumePort.loadRecentSalesVolume(any(), any(), any())).thenReturn(Map.of(
                storeA, new BigDecimal("1000.00"),
                storeB, new BigDecimal("100.00")
        ));

        job.computeRanking();

        var captor = ArgumentCaptor.forClass(List.class);
        verify(saveStoreRankingPort).replaceAll(captor.capture());
        @SuppressWarnings("unchecked")
        List<StoreRankingSnapshot> saved = captor.getValue();

        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getStoreId()).isEqualTo(storeA);
        assertThat(saved.get(0).getRank()).isEqualTo(1);
        assertThat(saved.get(0).getScore()).isEqualByComparingTo(new BigDecimal("1.000000"));
        assertThat(saved.get(1).getStoreId()).isEqualTo(storeB);
        assertThat(saved.get(1).getRank()).isEqualTo(2);
        assertThat(saved.get(1).getScore()).isEqualByComparingTo(new BigDecimal("0.300000"));
    }

    @Test
    void computeRanking_followerLoaderThrows_stillCompletesJobScoringFollowersAsZero() {
        // R4-001: a bulk cross-module call failing entirely (not a per-store exception) must
        // not abort the whole nightly run either.
        var storeA = UUID.randomUUID();
        when(listStoresUseCase.listActiveIds()).thenReturn(List.of(storeA));
        when(loadStoreRatingPort.loadSummaries(any())).thenReturn(Map.of(
                storeA, new StoreRatingSummary(4.0, 5L)
        ));
        when(getStoreFollowersUseCase.getFollowerCounts(any())).thenThrow(new RuntimeException("followers service down"));
        when(loadRecentSalesVolumePort.loadRecentSalesVolume(any(), any(), any())).thenReturn(Map.of());

        job.computeRanking();

        var captor = ArgumentCaptor.forClass(List.class);
        verify(saveStoreRankingPort).replaceAll(captor.capture());
        @SuppressWarnings("unchecked")
        List<StoreRankingSnapshot> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getStoreId()).isEqualTo(storeA);
        assertThat(saved.getFirst().getFollowerCount()).isEqualTo(0L);
    }

    @Test
    void computeRanking_ratingsLoaderThrows_completesJobWithoutAborting() {
        // Ratings determine eligibility (minReviews), so a total ratings-loader failure yields
        // an empty ranking rather than a crash — StoreRankingPersistenceAdapter.replaceAll is
        // then relied upon (R4-002) to not wipe existing data when handed an empty list.
        var storeA = UUID.randomUUID();
        when(listStoresUseCase.listActiveIds()).thenReturn(List.of(storeA));
        when(loadStoreRatingPort.loadSummaries(any())).thenThrow(new RuntimeException("ratings service down"));
        when(getStoreFollowersUseCase.getFollowerCounts(any())).thenReturn(Map.of());
        when(loadRecentSalesVolumePort.loadRecentSalesVolume(any(), any(), any())).thenReturn(Map.of());

        job.computeRanking();

        verify(saveStoreRankingPort).replaceAll(List.of());
    }

    @Test
    @SuppressWarnings("unchecked")
    void computeRanking_oneStoreFails_stillScoresTheOthers() {
        var healthy = UUID.randomUUID();
        var broken = UUID.randomUUID();
        when(listStoresUseCase.listActiveIds()).thenReturn(List.of(healthy, broken));
        when(loadStoreRatingPort.loadSummaries(any())).thenReturn(Map.of(
                healthy, new StoreRatingSummary(4.0, 5L),
                broken, new StoreRatingSummary(4.0, 5L)
        ));
        when(getStoreFollowersUseCase.getFollowerCounts(any())).thenReturn(Map.of());

        Map<UUID, BigDecimal> corruptSales = mock(Map.class);
        when(corruptSales.getOrDefault(healthy, BigDecimal.ZERO)).thenReturn(BigDecimal.TEN);
        when(corruptSales.getOrDefault(broken, BigDecimal.ZERO)).thenThrow(new RuntimeException("boom"));
        when(loadRecentSalesVolumePort.loadRecentSalesVolume(any(), any(), any())).thenReturn(corruptSales);

        job.computeRanking();

        var captor = ArgumentCaptor.forClass(List.class);
        verify(saveStoreRankingPort).replaceAll(captor.capture());
        List<StoreRankingSnapshot> saved = captor.getValue();

        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getStoreId()).isEqualTo(healthy);
    }
}
