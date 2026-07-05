package com.livecomerce.analytics.infrastructure.persistence;

import com.livecomerce.analytics.application.port.out.LoadLiveSummaryPort;
import com.livecomerce.analytics.application.port.out.LoadLiveSummarySourcePort;
import com.livecomerce.analytics.application.port.out.SaveLiveSummaryPort;
import com.livecomerce.analytics.domain.LiveReadEntity;
import com.livecomerce.analytics.domain.LiveSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Adapter tests use Mockito to mock the JPA repositories, avoiding the need
 * for an embedded database or Flyway migrations in tests (matches
 * LiveChatMessagePersistenceAdapterTest precedent).
 */
@ExtendWith(MockitoExtension.class)
class LiveSummaryPersistenceAdapterTest {

    @Mock LiveSummaryRepository liveSummaryRepository;
    @Mock LiveSummaryReadRepository liveSummaryReadRepository;
    @Mock LiveSummaryOrderDetailRepository liveSummaryOrderDetailRepository;

    @InjectMocks LiveSummaryPersistenceAdapter adapter;

    @Test
    void save_delegatesToRepository() {
        var summary = mock(LiveSummary.class);
        when(liveSummaryRepository.save(summary)).thenReturn(summary);

        var result = ((SaveLiveSummaryPort) adapter).save(summary);

        assertThat(result).isSameAs(summary);
        verify(liveSummaryRepository).save(summary);
    }

    @Test
    void existsByLiveId_delegatesToRepository() {
        var liveId = UUID.randomUUID();
        when(liveSummaryRepository.existsById(liveId)).thenReturn(true);

        assertThat(((SaveLiveSummaryPort) adapter).existsByLiveId(liveId)).isTrue();
        verify(liveSummaryRepository).existsById(liveId);
    }

    @Test
    void findByLiveId_delegatesToRepository() {
        var liveId = UUID.randomUUID();
        var summary = mock(LiveSummary.class);
        when(liveSummaryRepository.findById(liveId)).thenReturn(Optional.of(summary));

        var result = ((LoadLiveSummaryPort) adapter).findByLiveId(liveId);

        assertThat(result).contains(summary);
    }

    @Test
    void findLiveSnapshot_whenLiveExists_mapsFieldsFromLiveReadEntity() throws Exception {
        var liveId = UUID.randomUUID();
        var storeId = UUID.randomUUID();
        var startedAt = OffsetDateTime.now().minusHours(1);
        var endedAt = OffsetDateTime.now();
        var entity = buildLiveReadEntity(liveId, storeId, startedAt, endedAt, 99);
        when(liveSummaryReadRepository.findById(liveId)).thenReturn(Optional.of(entity));

        var result = ((LoadLiveSummarySourcePort) adapter).findLiveSnapshot(liveId);

        assertThat(result).isPresent();
        assertThat(result.get().storeId()).isEqualTo(storeId);
        assertThat(result.get().startedAt()).isEqualTo(startedAt);
        assertThat(result.get().endedAt()).isEqualTo(endedAt);
        assertThat(result.get().peakViewers()).isEqualTo(99);
    }

    @Test
    void findLiveSnapshot_whenLiveMissing_returnsEmpty() {
        var liveId = UUID.randomUUID();
        when(liveSummaryReadRepository.findById(liveId)).thenReturn(Optional.empty());

        var result = ((LoadLiveSummarySourcePort) adapter).findLiveSnapshot(liveId);

        assertThat(result).isEmpty();
    }

    @Test
    void findQualifyingOrders_mapsNativeQueryRowsToRecords() {
        var liveId = UUID.randomUUID();
        var orderId = UUID.randomUUID();
        var buyerId = UUID.randomUUID();
        Object[] row = { orderId, buyerId, new BigDecimal("150.00"), "T-Shirt, Mug" };
        when(liveSummaryOrderDetailRepository.findQualifyingOrderDetails(liveId))
                .thenReturn(List.<Object[]>of(row));

        var result = ((LoadLiveSummarySourcePort) adapter).findQualifyingOrders(liveId);

        assertThat(result).hasSize(1);
        var qualifyingOrder = result.get(0);
        assertThat(qualifyingOrder.orderId()).isEqualTo(orderId);
        assertThat(qualifyingOrder.buyerId()).isEqualTo(buyerId);
        assertThat(qualifyingOrder.orderTotal()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(qualifyingOrder.itemNames()).isEqualTo("T-Shirt, Mug");
    }

    @Test
    void findQualifyingOrders_whenNoRows_returnsEmptyList() {
        var liveId = UUID.randomUUID();
        when(liveSummaryOrderDetailRepository.findQualifyingOrderDetails(liveId))
                .thenReturn(List.of());

        var result = ((LoadLiveSummarySourcePort) adapter).findQualifyingOrders(liveId);

        assertThat(result).isEmpty();
    }

    private LiveReadEntity buildLiveReadEntity(UUID id, UUID storeId, OffsetDateTime startedAt,
                                                OffsetDateTime endedAt, int peakViewers) throws Exception {
        var constructor = LiveReadEntity.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var entity = constructor.newInstance();
        setField(entity, "id", id);
        setField(entity, "storeId", storeId);
        setField(entity, "startedAt", startedAt);
        setField(entity, "endedAt", endedAt);
        setField(entity, "peakViewers", peakViewers);
        return entity;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
